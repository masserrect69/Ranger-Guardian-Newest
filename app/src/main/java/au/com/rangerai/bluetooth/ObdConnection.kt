package au.com.rangerai.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import au.com.rangerai.data.DiagnosticTroubleCode
import au.com.rangerai.data.DtcStatus
import au.com.rangerai.data.decodeSupportedMode01Pids
import au.com.rangerai.data.parseDiagnosticTroubleCodes
import au.com.rangerai.data.mergeSupportedPidMaps
import au.com.rangerai.data.parseObdPayloads
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.UUID

class ObdConnection(private val context: Context) {

    companion object {
        private const val TAG = "ObdConnection"
        private const val RESPONSE_TIMEOUT_MS = 750L
        private const val RESPONSE_PENDING_DELAY_MS = 120L
        private const val INTER_BYTE_TIMEOUT_MS = 350L
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Initializing : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _supportedMode01Pids = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    /** Supported Mode 01 PIDs keyed by physical response ID (normally 7E8 and 7E9). */
    val supportedMode01Pids: StateFlow<Map<String, Set<String>>> = _supportedMode01Pids.asStateFlow()

    private val commandLock = Any()
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var inputStream: InputStream? = null
    @Volatile private var outputStream: OutputStream? = null

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager?.adapter }

    fun getPairedDevices(): List<BluetoothDevice> = try {
        bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    } catch (e: SecurityException) {
        Log.e(TAG, "Bluetooth permission not granted: ${e.message}")
        emptyList()
    }

    fun isConnected(): Boolean = socket?.isConnected == true

    suspend fun connect(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        _state.value = ConnectionState.Connecting
        closeSocket()
        _supportedMode01Pids.value = emptyMap()

        try { bluetoothAdapter?.cancelDiscovery() } catch (_: SecurityException) {}

        val attempts = listOf(
            "secure SPP" to { device.createRfcommSocketToServiceRecord(SPP_UUID) },
            "insecure SPP" to { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) },
            "RFCOMM channel 1" to { createReflectionSocket(device) }
        )

        var connected = false
        for ((label, factory) in attempts) {
            if (tryConnect(factory, label)) {
                connected = true
                break
            }
            delay(300L)
        }

        if (!connected) {
            _state.value = ConnectionState.Error(
                "Could not open the vLinker Bluetooth serial connection. Confirm it is paired, no other app is connected, and ignition is ON."
            )
            return@withContext false
        }

        _state.value = ConnectionState.Initializing
        if (initializeAdapter()) {
            _state.value = ConnectionState.Connected
            Log.i(TAG, "vLinker initialized; supported ECUs=${_supportedMode01Pids.value.keys}")
            true
        } else {
            closeSocket()
            _state.value = ConnectionState.Error(
                "Adapter connected but the vehicle did not complete ISO 15765-4 CAN initialization. Check ignition and close Torque/FORScan first."
            )
            false
        }
    }

    private fun createReflectionSocket(device: BluetoothDevice): BluetoothSocket {
        val method: Method = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
        return method.invoke(device, 1) as BluetoothSocket
    }

    private fun tryConnect(factory: () -> BluetoothSocket, label: String): Boolean {
        var candidate: BluetoothSocket? = null
        return try {
            candidate = factory()
            candidate.connect()
            socket = candidate
            inputStream = candidate.inputStream
            outputStream = candidate.outputStream
            Log.i(TAG, "Socket connected using $label")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "$label permission denied: ${e.message}")
            try { candidate?.close() } catch (_: IOException) {}
            false
        } catch (e: Exception) {
            Log.w(TAG, "$label failed: ${e.message}")
            try { candidate?.close() } catch (_: IOException) {}
            false
        }
    }

    private fun initializeAdapter(): Boolean {
        return try {
            sendRaw("ATZ", 3500L)
            Thread.sleep(900L)

            if (!commandAccepted("ATE0")) return false
            commandAccepted("ATL0")
            commandAccepted("ATS0")
            commandAccepted("ATH1")

            // User/Torque confirmed ISO 15765-4 CAN, 11-bit identifiers, 500 kbit/s.
            if (!commandAccepted("ATSP6", 2000L)) return false
            commandAccepted("ATCAF1")
            commandAccepted("ATAL")
            commandAccepted("ATAT2")
            commandAccepted("ATST32") // 0x32 * 4 ms = 200 ms; adaptive timing can shorten fast replies

            // Functional request discovers both 7E8 (ECM) and 7E9 (TCM).
            commandAccepted("ATSH7DF")
            val discovered = discoverSupportedMode01Pids()
            _supportedMode01Pids.value = discovered

            commandAccepted("ATSH7E0")
            val test = sendRaw("010C", 1800L)
            val hasPositiveResponse = parseObdPayloads(test).any { it.payloadHex.contains("410C") }
            if (!hasPositiveResponse) {
                Log.e(TAG, "ECM RPM test failed: $test")
                return false
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Adapter initialization exception: ${e.message}", e)
            false
        }
    }

    private fun commandAccepted(command: String, timeoutMs: Long = 1200L): Boolean {
        val response = sendRaw(command, timeoutMs).uppercase()
        val accepted = response.lineSequence().any { it.trim() == "OK" }
        if (!accepted) Log.w(TAG, "Adapter rejected $command: $response")
        return accepted
    }

    private fun discoverSupportedMode01Pids(): Map<String, Set<String>> {
        val discovered = linkedMapOf<String, MutableSet<String>>()
        val bases = listOf(0x00, 0x20, 0x40, 0x60, 0x80, 0xA0, 0xC0)

        for ((index, base) in bases.withIndex()) {
            val raw = sendRaw("01%02X".format(base), 1800L)
            val decoded = decodeSupportedMode01Pids(raw, base)
            if (decoded.isEmpty()) {
                if (base == 0x00) Log.w(TAG, "Supported-PID discovery returned no bitmap: $raw")
                break
            }
            mergeSupportedPidMaps(discovered, decoded)

            val nextBase = bases.getOrNull(index + 1) ?: break
            val nextBitmapPid = "%02X".format(nextBase)
            if (discovered.values.none { nextBitmapPid in it }) break
        }

        return discovered.mapValues { it.value.toSet() }
    }

    /**
     * Uses the discovered ECU bitmap to avoid sending unsupported Mode 01 requests.
     * If discovery failed, it fails open so a valid vehicle is not accidentally silenced.
     */
    fun isMode01PidSupported(pid: String, requestHeader: String?): Boolean {
        val map = _supportedMode01Pids.value
        if (map.isEmpty()) return true

        val responseHeader = when (requestHeader?.uppercase()) {
            "7E0" -> "7E8"
            "7E1" -> "7E9"
            else -> null
        }
        val normalizedPid = pid.removePrefix("01").uppercase().padStart(2, '0')
        return if (responseHeader != null) {
            map[responseHeader]?.contains(normalizedPid) == true
        } else {
            map.values.any { normalizedPid in it }
        }
    }

    private fun sendRaw(command: String, timeoutMs: Long): String = synchronized(commandLock) {
        try {
            val out = outputStream ?: return@synchronized "ERROR"
            val ins = inputStream ?: return@synchronized "ERROR"

            drainInput(ins)
            out.write("$command\r".toByteArray(Charsets.US_ASCII))
            out.flush()

            val sb = StringBuilder()
            val deadline = System.currentTimeMillis() + timeoutMs
            var lastByteTime = System.currentTimeMillis()

            while (System.currentTimeMillis() < deadline) {
                val available = try { ins.available() } catch (_: IOException) { break }
                if (available > 0) {
                    val b = try { ins.read() } catch (_: IOException) { break }
                    if (b == -1) break
                    val ch = b.toChar()
                    if (ch == '>') break
                    sb.append(ch)
                    lastByteTime = System.currentTimeMillis()
                } else {
                    if (sb.isNotEmpty() && System.currentTimeMillis() - lastByteTime >= INTER_BYTE_TIMEOUT_MS) break
                    Thread.sleep(4L)
                }
            }

            val result = normalizeElmResponse(sb.toString())
            if (!command.startsWith("01") && !command.startsWith("22")) {
                Log.v(TAG, "CMD: $command -> RSP: ${result.replace('\n', ' ')}")
            }
            result
        } catch (e: IOException) {
            Log.e(TAG, "I/O error sending '$command': ${e.message}")
            _state.value = ConnectionState.Error("Communication error: ${e.message}")
            "ERROR"
        }
    }

    private fun drainInput(ins: InputStream) {
        try {
            while (ins.available() > 0) ins.read()
        } catch (_: IOException) {}
    }

    private fun normalizeElmResponse(raw: String): String = raw
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .lineSequence()
        .map { it.trim().replace(Regex("\\s+"), " ") }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
        .trim()

    suspend fun sendRawCommand(command: String, timeoutMs: Long = RESPONSE_TIMEOUT_MS): String =
        withContext(Dispatchers.IO) { sendRaw(command, timeoutMs) }

    suspend fun sendRawAtCommand(command: String): String = sendRawCommand(command, 1500L)

    suspend fun sendMode22Request(did: String): String {
        val normalizedDid = did.uppercase()
        var raw = sendRaw("22$normalizedDid", RESPONSE_TIMEOUT_MS)
        if (containsNegativeResponse(raw, "22", "78") && !containsPositive(raw, "62$normalizedDid")) {
            delay(RESPONSE_PENDING_DELAY_MS)
            raw = sendRaw("22$normalizedDid", RESPONSE_TIMEOUT_MS)
        }
        return classifyResponse(raw, positivePrefix = "62$normalizedDid", service = "22")
    }

    suspend fun sendMode01Request(pid: String): String {
        val normalizedPid = pid.removePrefix("01").uppercase().padStart(2, '0')
        var raw = sendRaw("01$normalizedPid", RESPONSE_TIMEOUT_MS)
        if (containsNegativeResponse(raw, "01", "78") && !containsPositive(raw, "41$normalizedPid")) {
            delay(RESPONSE_PENDING_DELAY_MS)
            raw = sendRaw("01$normalizedPid", RESPONSE_TIMEOUT_MS)
        }
        return classifyResponse(raw, positivePrefix = "41$normalizedPid", service = "01")
    }


    /** Read stored, pending and permanent DTCs from all responding 11-bit ECUs. */
    suspend fun readDiagnosticTroubleCodes(): List<DiagnosticTroubleCode> = withContext(Dispatchers.IO) {
        commandAccepted("ATSH7DF")
        try {
            buildList {
                addAll(parseDiagnosticTroubleCodes(sendRaw("03", 2500L), DtcStatus.STORED))
                addAll(parseDiagnosticTroubleCodes(sendRaw("07", 2500L), DtcStatus.PENDING))
                addAll(parseDiagnosticTroubleCodes(sendRaw("0A", 2500L), DtcStatus.PERMANENT))
            }.distinctBy { Triple(it.code, it.status, it.responseHeader) }
        } finally {
            commandAccepted("ATSH7E0")
        }
    }

    private fun classifyResponse(raw: String, positivePrefix: String, service: String): String {
        val upper = raw.uppercase()
        return when {
            containsPositive(raw, positivePrefix) -> raw.uppercase().trim()
            upper.isBlank() || upper.contains("NO DATA") || upper.contains("STOPPED") -> "NO_DATA"
            upper.contains("ERROR") || upper == "?" -> "ERROR"
            containsNegativeResponse(raw, service) -> "NEGATIVE_RESPONSE"
            else -> "NO_DATA"
        }
    }

    private fun containsPositive(raw: String, prefix: String): Boolean =
        parseObdPayloads(raw).any { it.payloadHex.contains(prefix) }

    private fun containsNegativeResponse(raw: String, service: String, nrc: String? = null): Boolean {
        val prefix = "7F${service.uppercase()}"
        return parseObdPayloads(raw).any { payload ->
            val index = payload.payloadHex.indexOf(prefix)
            index >= 0 && (nrc == null || payload.payloadHex.drop(index + prefix.length).startsWith(nrc.uppercase()))
        }
    }

    private fun closeSocket() {
        try { inputStream?.close() } catch (_: IOException) {}
        try { outputStream?.close() } catch (_: IOException) {}
        try { socket?.close() } catch (_: IOException) {}
        inputStream = null
        outputStream = null
        socket = null
    }

    fun disconnect() {
        closeSocket()
        _supportedMode01Pids.value = emptyMap()
        _state.value = ConnectionState.Disconnected
    }
}
