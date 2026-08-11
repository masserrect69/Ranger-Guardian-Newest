package au.com.rangerai

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import au.com.rangerai.bluetooth.ObdViewModel
import au.com.rangerai.ui.FordGuardianNavHost
import au.com.rangerai.ui.theme.FordGuardianTheme

class MainActivity : ComponentActivity() {
    private lateinit var obdViewModel: ObdViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            Log.d("FordGuardian", "Permission ${entry.key}: ${if (entry.value) "GRANTED" else "DENIED"}")
        }
        if (hasRequiredBluetoothPermissions()) {
            Log.d("FordGuardian", "Bluetooth permissions granted — triggering auto-connect")
            obdViewModel.retryAutoConnect()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        obdViewModel = ViewModelProvider(this)[ObdViewModel::class.java]
        // Remove the legacy direct-OpenAI credential from earlier builds.
        getSharedPreferences(ObdViewModel.PREFS_NAME, 0)
            .edit()
            .remove("openai_api_key")
            .apply()

        requestRequiredPermissions()

        setContent {
            FordGuardianTheme {
                FordGuardianNavHost()
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("FordGuardian", "All permissions already granted — triggering auto-connect")
            obdViewModel.retryAutoConnect()
        }
    }

    private fun hasRequiredBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
