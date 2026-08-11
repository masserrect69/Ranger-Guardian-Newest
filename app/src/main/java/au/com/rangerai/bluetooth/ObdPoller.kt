package au.com.rangerai.bluetooth

import android.util.Log
import au.com.rangerai.data.PidDefinition
import au.com.rangerai.data.PidPriority
import au.com.rangerai.data.PidRegistry
import au.com.rangerai.data.VehicleState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One-request-at-a-time OBD scheduler.  The vLinker/Torque benchmark supplied
 * for this vehicle is roughly 10-16 PID requests/s, so frequency labels are
 * treated as desired refresh intervals rather than attempting impossible
 * full-list 10 Hz batches.
 */
class ObdPoller(
    private val connection: ObdConnection,
    private val pidRegistry: PidRegistry
) {
    companion object {
        private const val TAG = "ObdPoller"
        private const val DEFAULT_REQUEST_GAP_MS = 75L
        private const val MIN_REQUEST_GAP_MS = 50L
        private const val MAX_REQUEST_GAP_MS = 1000L
        private const val NO_DATA_BLACKLIST_THRESHOLD = 5
        private const val NEG_RESP_BLACKLIST_THRESHOLD = 3
        private const val BLACKLIST_RETRY_MS = 60_000L
        private const val MAX_CONSECUTIVE_ERRORS = 12
        private const val HEADER_SWITCH_DELAY_MS = 20L
    }

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    private val _lastPollInfo = MutableStateFlow("")
    val lastPollInfo: StateFlow<String> = _lastPollInfo.asStateFlow()

    private val _successfulPolls = MutableStateFlow(0)
    val successfulPolls: StateFlow<Int> = _successfulPolls.asStateFlow()

    private val _pollRate = MutableStateFlow(0.0)
    val pollRate: StateFlow<Double> = _pollRate.asStateFlow()

    private var pollingJob: Job? = null
    private val blacklistedUntil = linkedMapOf<String, Long>()
    private val noDataCount = linkedMapOf<String, Int>()
    private val negativeResponseCount = linkedMapOf<String, Int>()
    private val lastPolledAt = linkedMapOf<String, Long>()

    @Volatile private var requestGapMs: Long = DEFAULT_REQUEST_GAP_MS
    @Volatile private var experimentalPidsEnabled = false
    @Volatile private var experimentalPidKeys: Set<String> = emptySet()

    private var consecutiveErrors = 0
    private var totalSuccessfulPolls = 0
    private var currentHeader = "7E0"
    private var rateWindowStartedAt = 0L
    private var rateWindowRequests = 0

    fun setRequestGapMs(value: Int) {
        requestGapMs = value.toLong().coerceIn(MIN_REQUEST_GAP_MS, MAX_REQUEST_GAP_MS)
    }

    fun setExperimentalPidsEnabled(enabled: Boolean) {
        experimentalPidsEnabled = enabled
        if (!enabled) {
            lastPolledAt.keys.removeAll { it.startsWith("22_") }
        }
    }

    /** Only favourited Mode 22 DIDs are continuously polled to protect bus bandwidth. */
    fun setExperimentalPidKeys(keys: Set<String>) {
        experimentalPidKeys = keys.filterTo(linkedSetOf()) { it.startsWith("22_") }
    }

    fun startPolling(scope: CoroutineScope) {
        if (_isPolling.value) return
        resetSessionCounters()
        _isPolling.value = true

        pollingJob = scope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val pid = selectNextPid()
                    if (pid == null) {
                        delay(250L)
                        continue
                    }

                    val header = pid.ecuHeader ?: "7E0"
                    if (header != currentHeader) switchHeader(header)

                    pollSinglePid(pid)
                    lastPolledAt[pid.key] = System.currentTimeMillis()
                    updatePollRate()
                    delay(requestGapMs)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Polling error: ${e.message}", e)
                    consecutiveErrors++
                    if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                        _lastPollInfo.value = "Polling stopped after repeated communication errors"
                        break
                    }
                    delay(500L)
                }
            }
            _isPolling.value = false
        }
    }

    fun stopPolling(clearValues: Boolean = false) {
        pollingJob?.cancel()
        pollingJob = null
        _isPolling.value = false
        if (clearValues) _vehicleState.value = _vehicleState.value.clearLiveData()
    }


    /** Call after an out-of-band command changes the ELM request header. */
    fun resetHeader(header: String = "7E0") {
        currentHeader = header.uppercase()
    }

    private fun resetSessionCounters() {
        blacklistedUntil.clear()
        noDataCount.clear()
        negativeResponseCount.clear()
        lastPolledAt.clear()
        consecutiveErrors = 0
        totalSuccessfulPolls = 0
        currentHeader = "7E0"
        rateWindowStartedAt = System.currentTimeMillis()
        rateWindowRequests = 0
        _successfulPolls.value = 0
        _pollRate.value = 0.0
    }

    private fun enabledPids(): List<PidDefinition> {
        val now = System.currentTimeMillis()
        blacklistedUntil.entries.removeAll { it.value <= now }

        return pidRegistry.allPids.filter { pid ->
            val notBlacklisted = blacklistedUntil[pid.key]?.let { it > now } != true
            if (!notBlacklisted) return@filter false

            if (pid.obdMode == 1) {
                val pidHex = pid.did.removePrefix("01")
                connection.isMode01PidSupported(pidHex, pid.ecuHeader)
            } else {
                experimentalPidsEnabled && pid.key in experimentalPidKeys
            }
        }
    }

    private fun selectNextPid(): PidDefinition? {
        val now = System.currentTimeMillis()
        return enabledPids().minByOrNull { pid ->
            val last = lastPolledAt[pid.key] ?: 0L
            last + desiredIntervalMs(pid)
        }?.takeIf { pid ->
            val last = lastPolledAt[pid.key] ?: 0L
            now >= last + desiredIntervalMs(pid)
        }
    }

    private fun desiredIntervalMs(pid: PidDefinition): Long {
        if (pid.obdMode == 0) {
            return when (pid.priority) {
                PidPriority.HIGH -> 1_000L
                PidPriority.MEDIUM -> 3_000L
                PidPriority.LOW -> 10_000L
            }
        }
        return when (pid.priority) {
            PidPriority.HIGH -> 500L
            PidPriority.MEDIUM -> 2_000L
            PidPriority.LOW -> 10_000L
        }
    }

    private suspend fun switchHeader(header: String) {
        val response = connection.sendRawAtCommand("ATSH$header")
        if (response.uppercase().contains("ERROR")) {
            throw IllegalStateException("Could not switch request header to $header")
        }
        currentHeader = header
        delay(HEADER_SWITCH_DELAY_MS)
    }

    private suspend fun pollSinglePid(pid: PidDefinition) {
        rateWindowRequests++
        val rawResponse = if (pid.obdMode == 1) {
            connection.sendMode01Request(pid.did.removePrefix("01"))
        } else {
            connection.sendMode22Request(pid.did)
        }

        when (rawResponse) {
            "NO_DATA" -> registerNoData(pid)
            "NEGATIVE_RESPONSE" -> registerNegativeResponse(pid)
            "ERROR" -> consecutiveErrors++
            else -> {
                consecutiveErrors = 0
                noDataCount.remove(pid.key)
                negativeResponseCount.remove(pid.key)

                val decoded = pid.decode(rawResponse)
                when {
                    decoded == null -> _lastPollInfo.value = "${pid.description}: incomplete response"
                    !pid.isPlausible(decoded) -> {
                        Log.w(TAG, "Rejected implausible ${pid.name}=$decoded ${pid.unit}")
                        _lastPollInfo.value = "Rejected implausible ${pid.description} value"
                    }
                    else -> {
                        totalSuccessfulPolls++
                        _successfulPolls.value = totalSuccessfulPolls
                        _vehicleState.value = _vehicleState.value.updateParameter(pid.name, decoded)
                        _lastPollInfo.value = "${pid.description}: ${formatValue(decoded)} ${pid.unit}".trim()
                    }
                }
            }
        }
    }

    private fun registerNoData(pid: PidDefinition) {
        val count = (noDataCount[pid.key] ?: 0) + 1
        noDataCount[pid.key] = count
        if (count >= NO_DATA_BLACKLIST_THRESHOLD) blacklist(pid, "NO DATA")
    }

    private fun registerNegativeResponse(pid: PidDefinition) {
        val count = (negativeResponseCount[pid.key] ?: 0) + 1
        negativeResponseCount[pid.key] = count
        if (count >= NEG_RESP_BLACKLIST_THRESHOLD) blacklist(pid, "negative response")
    }

    private fun blacklist(pid: PidDefinition, reason: String) {
        blacklistedUntil[pid.key] = System.currentTimeMillis() + BLACKLIST_RETRY_MS
        noDataCount.remove(pid.key)
        negativeResponseCount.remove(pid.key)
        Log.w(TAG, "Temporarily paused ${pid.name}: $reason")
    }

    private fun updatePollRate() {
        val now = System.currentTimeMillis()
        val elapsed = now - rateWindowStartedAt
        if (elapsed >= 2_000L) {
            _pollRate.value = rateWindowRequests * 1000.0 / elapsed.toDouble()
            rateWindowStartedAt = now
            rateWindowRequests = 0
        }
    }

    private fun formatValue(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

    fun getCurrentState(): VehicleState = _vehicleState.value

    fun resetNonRespondingPids() {
        blacklistedUntil.clear()
        noDataCount.clear()
        negativeResponseCount.clear()
    }

    fun getDebugInfo(): String = buildString {
        appendLine("Rate: ${"%.1f".format(_pollRate.value)} PID/s | OK: $totalSuccessfulPolls")
        appendLine("Header: $currentHeader | Gap: ${requestGapMs}ms | Errors: $consecutiveErrors")
        appendLine("Supported ECUs: ${connection.supportedMode01Pids.value.keys.joinToString().ifEmpty { "unknown" }}")
        append("Paused PIDs: ${blacklistedUntil.size} | Experimental: ${if (experimentalPidsEnabled) experimentalPidKeys.size else 0}")
    }
}
