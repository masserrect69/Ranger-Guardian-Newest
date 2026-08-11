package au.com.rangerai.data

import android.util.Log

/**
 * One pollable OBD parameter.  [minValue] and [maxValue] are also used as a
 * plausibility guard: a decoded value outside this range is not published.
 */
data class PidDefinition(
    val name: String,
    val did: String,
    val description: String,
    val unit: String,
    val category: PidCategory,
    val priority: PidPriority,
    val minValue: Double = 0.0,
    val maxValue: Double = 100.0,
    val decoder: PidDecoder,
    val ecuHeader: String? = null,
    val obdMode: Int = 0   // 0 = Mode 22, 1 = Mode 01
) {
    val key: String get() = "${if (obdMode == 1) "01" else "22"}_$did"
    val isVerifiedStandard: Boolean get() = obdMode == 1
    val isExperimentalFord: Boolean get() = obdMode == 0

    fun decode(rawResponse: String): Double? = decoder.decode(rawResponse, did)

    fun isPlausible(value: Double): Boolean =
        value.isFinite() && value >= minValue && value <= maxValue
}

private data class CanFrame(val header: String?, val bytes: ByteArray)
private data class IsoTpAssembly(val expectedLength: Int, val data: MutableList<Byte>)

data class ParsedObdPayload(
    val responseHeader: String?,
    val payloadHex: String
)

private val HEX_ONLY = Regex("^[0-9A-F]+$")
private val CAN_HEADER = Regex("^[0-9A-F]{3}$")
private val NUMBERED_LINE = Regex("(?=\\b[0-9A-F]{1,2}:\\s*)")

/**
 * Convert common ELM/vLinker output variants into one or more application
 * payloads.  Supported forms include:
 *
 *  - `41 05 3C`
 *  - `7E8 03 41 05 3C`
 *  - `7E80341053C` (headers/spaces off)
 *  - numbered ISO-TP lines such as `0: 10 14 62 ...` / `1: 21 ...`
 *
 * Multiple ECU replies (for example 7E8 and 7E9) are kept as separate
 * payloads so the caller can select the one containing the expected echo.
 */
fun parseObdPayloads(rawResponse: String): List<ParsedObdPayload> {
    val raw = rawResponse
        .replace('>', '\n')
        .replace("$", "")
        .replace(Regex("(?i)SEARCHING\\.\\.\\."), "")
        .replace(Regex("(?i)BUS INIT[^\\r\\n]*"), "")

    val physicalLines = raw
        .split(Regex("[\\r\\n]+"))
        .flatMap { line ->
            // Some callers historically flattened ELM line breaks into spaces.
            // Split numbered ISO-TP records back into individual lines.
            if (line.contains(Regex("\\b[0-9A-F]{1,2}:\\s*"))) {
                line.split(NUMBERED_LINE)
            } else listOf(line)
        }

    val frames = mutableListOf<CanFrame>()

    for (originalLine in physicalLines) {
        var line = originalLine.trim()
        if (line.isEmpty()) continue

        line = line.replace(Regex("^[0-9A-F]{1,2}:\\s*", RegexOption.IGNORE_CASE), "")
        line = line.replace(Regex("(?i)0X"), "")

        val tokens = line.uppercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        var header: String? = null
        var hex: String

        if (tokens.size > 1 && CAN_HEADER.matches(tokens.first())) {
            header = tokens.first()
            hex = tokens.drop(1).joinToString("")
        } else {
            hex = tokens.joinToString("")
            // ATH1 + ATS0 output, e.g. 7E80341053C.  A 3-nibble CAN ID
            // makes the total character count odd, which is a reliable cue.
            if (hex.length >= 9 && hex.length % 2 == 1 && CAN_HEADER.matches(hex.take(3))) {
                header = hex.take(3)
                hex = hex.drop(3)
            }
        }

        hex = hex.filter { it in '0'..'9' || it in 'A'..'F' }
        if (hex.length < 2 || !HEX_ONLY.matches(hex)) continue
        if (hex.length % 2 == 1) hex = hex.dropLast(1)

        val bytes = runCatching {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }.getOrNull() ?: continue

        if (bytes.isNotEmpty()) frames += CanFrame(header, bytes)
    }

    if (frames.isEmpty()) return emptyList()

    val payloads = mutableListOf<ParsedObdPayload>()
    val assemblies = mutableMapOf<String, IsoTpAssembly>()

    fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    fun MutableList<Byte>.toHex(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }

    frames.forEachIndexed { index, frame ->
        val bytes = frame.bytes
        if (bytes.isEmpty()) return@forEachIndexed
        val pci = bytes[0].toInt() and 0xFF
        val frameType = pci ushr 4
        val assemblyKey = frame.header ?: "NO_HEADER_$index"

        when (frameType) {
            0x0 -> {
                val declaredLength = pci and 0x0F
                if (declaredLength > 0 && bytes.size >= declaredLength + 1) {
                    payloads += ParsedObdPayload(frame.header, bytes.copyOfRange(1, 1 + declaredLength).toHex())
                } else {
                    // Torque can export an already reassembled long response with
                    // a leading 00.  Keep the remaining bytes instead of dropping it.
                    val candidate = if (pci == 0 && bytes.size > 1) bytes.copyOfRange(1, bytes.size) else bytes
                    payloads += ParsedObdPayload(frame.header, candidate.toHex())
                }
            }
            0x1 -> {
                if (bytes.size < 2) return@forEachIndexed
                val expected = ((pci and 0x0F) shl 8) or (bytes[1].toInt() and 0xFF)
                val data = bytes.copyOfRange(2, bytes.size).toMutableList()
                assemblies[assemblyKey] = IsoTpAssembly(expected, data)
                if (data.size >= expected) {
                    payloads += ParsedObdPayload(frame.header, data.take(expected).toByteArray().toHex())
                    assemblies.remove(assemblyKey)
                }
            }
            0x2 -> {
                // If the adapter omitted headers, attach to the sole open assembly.
                val key = if (frame.header != null) assemblyKey
                else assemblies.keys.singleOrNull() ?: assemblyKey
                val assembly = assemblies[key] ?: return@forEachIndexed
                assembly.data += bytes.copyOfRange(1, bytes.size).toList()
                if (assembly.data.size >= assembly.expectedLength) {
                    payloads += ParsedObdPayload(frame.header, assembly.data.take(assembly.expectedLength).toByteArray().toHex())
                    assemblies.remove(key)
                }
            }
            else -> payloads += ParsedObdPayload(frame.header, bytes.toHex())
        }
    }

    // Do not silently discard a partially received ISO-TP message.  Returning
    // it allows the prefix/length checks in decoders to reject it cleanly and
    // produces better diagnostics than pretending the response was empty.
    assemblies.forEach { (key, assembly) ->
        if (assembly.data.isNotEmpty()) {
            val header = key.takeUnless { it.startsWith("NO_HEADER_") }
            payloads += ParsedObdPayload(header, assembly.data.toHex())
        }
    }

    return payloads.distinctBy { it.responseHeader to it.payloadHex }
}

/**
 * Reassemble the first ELM/ISO-TP payload.  Retained for diagnostics and tests;
 * normal decoding should use [extractDataBytes].
 */
fun reassembleMultiFrame(rawResponse: String): String =
    parseObdPayloads(rawResponse).firstOrNull()?.payloadHex ?: rawResponse.trim()

/**
 * Extract data bytes after the positive-response echo:
 *  - Mode 22: `62 + DID`
 *  - Mode 01: `41 + PID`
 */
fun extractAllDataBytes(rawResponse: String, did: String): List<ByteArray> {
    val normalizedDid = did.uppercase()
    val pid = if (normalizedDid.length == 4 && normalizedDid.startsWith("01")) {
        normalizedDid.substring(2)
    } else normalizedDid

    val prefixes = buildList {
        if (normalizedDid.length == 4 && normalizedDid.startsWith("01")) add("41$pid")
        else {
            add("62$normalizedDid")
            if (normalizedDid.length == 2) add("41$normalizedDid")
        }
    }

    val results = mutableListOf<ByteArray>()
    val payloads = parseObdPayloads(rawResponse)
    for (parsedPayload in payloads) {
        val payload = parsedPayload.payloadHex
        for (responsePrefix in prefixes) {
            val index = payload.indexOf(responsePrefix)
            if (index < 0) continue
            val dataHex = payload.substring(index + responsePrefix.length)
            if (dataHex.length < 2 || dataHex.length % 2 != 0) continue
            val parsed = runCatching {
                dataHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            }.getOrElse {
                Log.w("PidDecode", "Failed to parse '$dataHex' for DID=$did: ${it.message}")
                ByteArray(0)
            }
            if (parsed.isNotEmpty()) results += parsed
            break
        }
    }
    return results
}

fun extractDataBytes(rawResponse: String, did: String): ByteArray {
    val result = extractAllDataBytes(rawResponse, did).firstOrNull()
    if (result == null) {
        Log.w("PidDecode", "No positive response for DID=$did in '${rawResponse.take(160)}'")
    }
    return result ?: ByteArray(0)
}
