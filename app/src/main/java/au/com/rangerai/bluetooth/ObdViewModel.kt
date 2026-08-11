package au.com.rangerai.bluetooth

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.rangerai.data.DiagnosticTroubleCode
import au.com.rangerai.data.PidDefinition
import au.com.rangerai.data.PidRegistry
import au.com.rangerai.data.VehicleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ObdViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ObdViewModel"
        const val PREFS_NAME = "ford_guardian_prefs"
        private const val KEY_FAVOURITES = "dashboard_favourites"
        const val KEY_AUTO_CONNECT = "auto_connect"
        const val KEY_POLL_INTERVAL_MS = "poll_interval_ms"
        const val KEY_EXPERIMENTAL_PIDS = "experimental_ford_pids"

        private val DEFAULT_FAVOURITES = setOf(
            "01_010C", // RPM
            "01_0105", // coolant
            "01_010B", // MAP
            "01_010D", // speed
            "01_0110", // MAF
            "01_0111", // throttle
            "01_0142", // module voltage
            "01_0104"  // calculated load
        )

        private val OBD_ADAPTER_KEYWORDS = listOf(
            "vlinker", "obdlink", "elm327", "obd", "obdii", "carista",
            "veepeak", "bafx", "bluedriver", "konnwei", "ancel", "icar",
            "scantool", "foseal", "vgate", "xtool"
        )
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, 0)
    private val obdConnection = ObdConnection(application.applicationContext)
    val pidRegistry = PidRegistry()
    private val obdPoller = ObdPoller(obdConnection, pidRegistry)
    private val connectionAttemptInProgress = AtomicBoolean(false)

    val connectionState: StateFlow<ObdConnection.ConnectionState> = obdConnection.state
    val vehicleState: StateFlow<VehicleState> = obdPoller.vehicleState
    val isPolling: StateFlow<Boolean> = obdPoller.isPolling
    val lastPollInfo: StateFlow<String> = obdPoller.lastPollInfo
    val successfulPolls: StateFlow<Int> = obdPoller.successfulPolls
    val pollRate: StateFlow<Double> = obdPoller.pollRate
    val supportedMode01Pids: StateFlow<Map<String, Set<String>>> = obdConnection.supportedMode01Pids

    val isConnected: StateFlow<Boolean> = connectionState.map { state ->
        state is ObdConnection.ConnectionState.Connected
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _selectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val selectedDevice: StateFlow<BluetoothDevice?> = _selectedDevice.asStateFlow()

    private val _uiError = MutableStateFlow<String?>(null)
    val uiError: StateFlow<String?> = _uiError.asStateFlow()

    private val _favourites = MutableStateFlow<Set<String>>(loadFavourites())
    val favourites: StateFlow<Set<String>> = _favourites.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()


    private val _diagnosticTroubleCodes = MutableStateFlow<List<DiagnosticTroubleCode>>(emptyList())
    val diagnosticTroubleCodes: StateFlow<List<DiagnosticTroubleCode>> = _diagnosticTroubleCodes.asStateFlow()

    private val _isReadingDtcs = MutableStateFlow(false)
    val isReadingDtcs: StateFlow<Boolean> = _isReadingDtcs.asStateFlow()

    init {
        applyPollingPreferences()

        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    is ObdConnection.ConnectionState.Connected -> {
                        if (!isPolling.value) obdPoller.startPolling(viewModelScope)
                    }
                    is ObdConnection.ConnectionState.Disconnected,
                    is ObdConnection.ConnectionState.Error -> {
                        obdPoller.stopPolling(clearValues = true)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun applyPollingPreferences() {
        obdPoller.setRequestGapMs(prefs.getInt(KEY_POLL_INTERVAL_MS, 75))
        obdPoller.setExperimentalPidsEnabled(prefs.getBoolean(KEY_EXPERIMENTAL_PIDS, false))
        obdPoller.setExperimentalPidKeys(_favourites.value)
    }

    fun setPollIntervalMs(value: Int) {
        prefs.edit().putInt(KEY_POLL_INTERVAL_MS, value).apply()
        obdPoller.setRequestGapMs(value)
    }

    fun setExperimentalPidsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EXPERIMENTAL_PIDS, enabled).apply()
        obdPoller.setExperimentalPidsEnabled(enabled)
    }

    fun getPairedDevices(): List<BluetoothDevice> = obdConnection.getPairedDevices()

    fun findObdAdapter(): BluetoothDevice? {
        val preferredAddress = prefs.getString("preferred_device", null)
        if (!preferredAddress.isNullOrEmpty()) {
            getPairedDevices().firstOrNull { it.address == preferredAddress }?.let { return it }
        }

        return getPairedDevices().firstOrNull { device ->
            val name = try { device.name?.lowercase() } catch (_: SecurityException) { null }
                ?: return@firstOrNull false
            OBD_ADAPTER_KEYWORDS.any(name::contains)
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (connectionState.value !is ObdConnection.ConnectionState.Disconnected &&
            connectionState.value !is ObdConnection.ConnectionState.Error
        ) return
        if (!connectionAttemptInProgress.compareAndSet(false, true)) return

        _selectedDevice.value = device
        _uiError.value = null
        viewModelScope.launch {
            try {
                val success = obdConnection.connect(device)
                if (!success) {
                    _uiError.value = (connectionState.value as? ObdConnection.ConnectionState.Error)?.message
                } else {
                    _connectedDeviceName.value = try { device.name ?: "OBD Adapter" }
                    catch (_: SecurityException) { "OBD Adapter" }
                }
            } finally {
                connectionAttemptInProgress.set(false)
            }
        }
    }

    fun autoConnect() {
        if (connectionState.value !is ObdConnection.ConnectionState.Disconnected &&
            connectionState.value !is ObdConnection.ConnectionState.Error
        ) return

        val adapter = findObdAdapter()
        if (adapter != null) {
            connectToDevice(adapter)
        } else {
            val paired = getPairedDevices()
            val names = paired.mapNotNull { device ->
                try { device.name } catch (_: SecurityException) { null }
            }.joinToString(", ")
            _uiError.value = if (paired.isEmpty()) {
                "No paired Bluetooth devices found. Pair the vLinker FS in Android Bluetooth settings first."
            } else {
                "Could not identify an OBD adapter among ${paired.size} paired devices${if (names.isNotBlank()) " ($names)" else ""}. Select it in Settings."
            }
        }
    }

    fun retryAutoConnect() {
        _uiError.value = null
        if (!prefs.getBoolean(KEY_AUTO_CONNECT, true)) return
        if (connectionState.value is ObdConnection.ConnectionState.Disconnected ||
            connectionState.value is ObdConnection.ConnectionState.Error
        ) autoConnect()
    }

    fun clearError() { _uiError.value = null }

    fun disconnect() {
        obdPoller.stopPolling(clearValues = true)
        obdConnection.disconnect()
        _connectedDeviceName.value = null
    }

    fun resetBlacklist() = obdPoller.resetNonRespondingPids()
    fun getDebugInfo(): String = obdPoller.getDebugInfo()

    private fun loadFavourites(): Set<String> =
        prefs.getStringSet(KEY_FAVOURITES, null)?.toSet() ?: DEFAULT_FAVOURITES

    fun toggleFavourite(pidKey: String) {
        val current = _favourites.value.toMutableSet()
        if (!current.add(pidKey)) current.remove(pidKey)
        _favourites.value = current.toSet()
        prefs.edit().putStringSet(KEY_FAVOURITES, current).apply()
        obdPoller.setExperimentalPidKeys(current)
    }

    fun isFavourite(pidKey: String): Boolean = pidKey in _favourites.value

    fun getFavouritePids(): List<PidDefinition> =
        pidRegistry.allPids.filter { it.key in _favourites.value }

    suspend fun sendCommand(command: String, timeoutMs: Long = 3000L): String =
        obdConnection.sendRawCommand(command, timeoutMs)

    suspend fun sendMode22(did: String): String = obdConnection.sendMode22Request(did)
    suspend fun sendMode01(pid: String): String = obdConnection.sendMode01Request(pid)
    suspend fun requestVin(): String = obdConnection.sendRawCommand("0902", 5000L)

    fun refreshDiagnosticTroubleCodes() {
        if (!isConnected.value || _isReadingDtcs.value) return
        viewModelScope.launch {
            _isReadingDtcs.value = true
            val resumePolling = isPolling.value
            if (resumePolling) obdPoller.stopPolling(clearValues = false)
            try {
                _diagnosticTroubleCodes.value = obdConnection.readDiagnosticTroubleCodes()
            } catch (e: Exception) {
                Log.e(TAG, "DTC scan failed", e)
                _uiError.value = "Could not read diagnostic trouble codes: ${e.message ?: "communication error"}"
            } finally {
                obdPoller.resetHeader("7E0")
                if (resumePolling && isConnected.value) obdPoller.startPolling(viewModelScope)
                _isReadingDtcs.value = false
            }
        }
    }

    override fun onCleared() {
        Log.d(TAG, "Clearing OBD session")
        obdPoller.stopPolling(clearValues = true)
        obdConnection.disconnect()
        super.onCleared()
    }
}
