package au.com.rangerai.ui.chat

import au.com.rangerai.data.DiagnosticTroubleCode
import au.com.rangerai.data.PidRegistry
import au.com.rangerai.data.VehicleState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class AiSource(
    val kind: String,
    val title: String,
    val url: String? = null
)

data class AiAnswer(
    val text: String,
    val sources: List<AiSource> = emptyList()
)

object AiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun ask(
        backendUrl: String,
        clientToken: String,
        userMessage: String,
        history: List<ChatMessage>,
        vehicleState: VehicleState,
        isConnected: Boolean,
        pidRegistry: PidRegistry,
        diagnosticTroubleCodes: List<DiagnosticTroubleCode>,
        supportedMode01Pids: Map<String, Set<String>>
    ): AiAnswer = withContext(Dispatchers.IO) {
        val endpoint = normalizeEndpoint(backendUrl)
            ?: return@withContext AiAnswer("Configure your secure AI server using the server icon at the top right.")

        val metadataByName = pidRegistry.allPids.associateBy { it.name }
        val now = System.currentTimeMillis()
        val readings = JSONArray()
        vehicleState.freshParameters().entries
            .sortedBy { it.key }
            .take(120)
            .forEach { (key, value) ->
                val pid = metadataByName[key]
                readings.put(
                    JSONObject()
                        .put("key", key)
                        .put("did", pid?.did ?: JSONObject.NULL)
                        .put("description", pid?.description ?: key)
                        .put("value", value)
                        .put("unit", pid?.unit ?: "")
                        .put("ecu_request_header", pid?.ecuHeader ?: JSONObject.NULL)
                        .put("obd_mode", pid?.obdMode ?: JSONObject.NULL)
                        .put("age_ms", vehicleState.ageMs(key, now) ?: JSONObject.NULL)
                        .put("fresh", vehicleState.isFresh(key))
                        .put("sourceClass", when {
                            pid == null -> "unknown"
                            pid.isVerifiedStandard -> "verified_mode_01"
                            else -> "experimental_ford_mode_22"
                        })
                )
            }

        val historyJson = JSONArray()
        history.takeLast(20)
            .filter { !it.isLoading && it.content.isNotBlank() }
            .forEach { message ->
                historyJson.put(
                    JSONObject()
                        .put("role", message.role)
                        .put("content", message.content)
                )
            }

        val dtcs = JSONArray().apply {
            diagnosticTroubleCodes.take(80).forEach { dtc ->
                put(
                    JSONObject()
                        .put("code", dtc.code)
                        .put("status", dtc.status.label)
                        .put("responseEcu", dtc.responseHeader ?: JSONObject.NULL)
                )
            }
        }

        val supportedEcus = JSONObject().apply {
            supportedMode01Pids.toSortedMap().forEach { (ecu, pids) ->
                put(ecu, JSONArray(pids.sorted()))
            }
        }

        fun fresh(name: String): Double? = vehicleState.freshValue(name)
        val operatingState = JSONObject()
            .put("sampleTimeEpochMs", now)
            .put("rpm", fresh("RPM_01") ?: JSONObject.NULL)
            .put("speedKph", fresh("VSS") ?: JSONObject.NULL)
            .put("coolantC", fresh("ECT_01") ?: JSONObject.NULL)
            .put("engineLoadPct", fresh("ENG_LOAD") ?: JSONObject.NULL)
            .put("mapKpaAbsolute", fresh("MAP_01") ?: JSONObject.NULL)
            .put("mafGps", fresh("MAF_01") ?: JSONObject.NULL)
            .put("batteryV", fresh("BATT_V_01") ?: JSONObject.NULL)
            .put("commandedEgrPct", fresh("EGR_CMD_01") ?: JSONObject.NULL)

        val body = JSONObject()
            .put("message", userMessage)
            .put("history", historyJson)
            .put(
                "vehicle",
                JSONObject()
                    .put("year", 2018)
                    .put("make", "Ford")
                    .put("model", "Ranger PX MkII")
                    .put("engine", "3.2L five-cylinder Duratorq/Puma diesel")
                    .put("transmission", "6R80 automatic")
                    .put("market", "Australia")
                    .put("protocol", "ISO 15765-4 CAN 11-bit 500 kbit/s")
                    .put("connected", isConnected)
            )
            .put("operatingState", operatingState)
            .put("supportedEcus", supportedEcus)
            .put("dtcs", dtcs)
            .put("readings", readings)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val builder = Request.Builder()
            .url(endpoint)
            .header("Content-Type", "application/json")
            .post(body)
        if (clientToken.isNotBlank()) builder.header("Authorization", "Bearer ${clientToken.trim()}")

        try {
            client.newCall(builder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext AiAnswer(
                        when (response.code) {
                            401, 403 -> "The AI server rejected the app token. Check the server settings."
                            429 -> "The AI service rate limit has been reached. Try again shortly."
                            else -> "AI server error ${response.code}: ${extractError(responseBody)}"
                        }
                    )
                }
                val json = JSONObject(responseBody)
                val answer = json.optString("answer").takeIf { it.isNotBlank() }
                    ?: "The AI server returned an empty answer."
                val sourcesJson = json.optJSONArray("sources") ?: JSONArray()
                val sources = buildList {
                    for (i in 0 until sourcesJson.length()) {
                        val source = sourcesJson.optJSONObject(i) ?: continue
                        val title = source.optString("title").ifBlank { "Ranger reference" }
                        add(
                            AiSource(
                                kind = source.optString("kind").ifBlank { "reference" },
                                title = title,
                                url = source.optString("url").takeIf { it.startsWith("https://") }
                            )
                        )
                    }
                }
                AiAnswer(answer, sources)
            }
        } catch (e: IOException) {
            AiAnswer("Could not reach the AI server: ${e.message ?: "network error"}")
        } catch (e: Exception) {
            AiAnswer("AI response error: ${e.message ?: "unknown error"}")
        }
    }

    private fun normalizeEndpoint(value: String): String? {
        val base = value.trim().trimEnd('/')
        if (base.isBlank()) return null
        if (!base.startsWith("https://") && !base.startsWith("http://10.0.2.2")) return null
        return if (base.endsWith("/v1/diagnose")) base else "$base/v1/diagnose"
    }

    private fun extractError(body: String): String = runCatching {
        JSONObject(body).optString("error").ifBlank { "request failed" }
    }.getOrDefault("request failed")
}
