package au.com.rangerai.ui.settings

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import au.com.rangerai.BuildConfig
import au.com.rangerai.bluetooth.ObdViewModel
import au.com.rangerai.ui.theme.*

@Composable
fun SettingsScreen(navController: NavController, obdViewModel: ObdViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(ObdViewModel.PREFS_NAME, 0) }
    val connectedDevice by obdViewModel.connectedDeviceName.collectAsState()
    val isConnected by obdViewModel.isConnected.collectAsState()

    var autoConnect by remember { mutableStateOf(prefs.getBoolean(ObdViewModel.KEY_AUTO_CONNECT, true)) }
    var preferredDeviceAddress by remember { mutableStateOf(prefs.getString("preferred_device", "") ?: "") }
    var pollIntervalMs by remember { mutableStateOf(prefs.getInt(ObdViewModel.KEY_POLL_INTERVAL_MS, 75)) }
    var experimentalPids by remember { mutableStateOf(prefs.getBoolean(ObdViewModel.KEY_EXPERIMENTAL_PIDS, false)) }
    var showDevicePicker by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val pairedDevices = remember {
        try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bm?.adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) { emptyList() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = IceBlueGlow, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("Settings", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── CONNECTION ──
            item {
                SectionHeader("BLUETOOTH CONNECTION")
            }

            item {
                // Current connection status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.BluetoothDisabled,
                            contentDescription = null,
                            tint = if (isConnected) AccentGreen else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isConnected) "Connected" else "Disconnected",
                                color = if (isConnected) AccentGreen else TextMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                connectedDevice ?: "No device",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (isConnected) {
                            TextButton(onClick = { obdViewModel.disconnect() }) {
                                Text("Disconnect", color = AccentRed, fontSize = 12.sp)
                            }
                        } else {
                            TextButton(onClick = { obdViewModel.autoConnect() }) {
                                Text("Connect", color = IceBlueGlow, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                SettingRow(
                    icon = Icons.Filled.Devices,
                    title = "OBD Adapter",
                    subtitle = if (preferredDeviceAddress.isEmpty()) "Tap to select a paired device"
                               else pairedDevices.firstOrNull { it.address == preferredDeviceAddress }
                                   ?.let { try { it.name } catch (e: SecurityException) { it.address } }
                                   ?: preferredDeviceAddress,
                    onClick = { showDevicePicker = true }
                )
            }

            item {
                SettingToggleRow(
                    icon = Icons.Filled.Autorenew,
                    title = "Auto-connect on launch",
                    subtitle = "Automatically connect to preferred device",
                    checked = autoConnect,
                    onCheckedChange = {
                        autoConnect = it
                        prefs.edit().putBoolean(ObdViewModel.KEY_AUTO_CONNECT, it).apply()
                    }
                )
            }

            // ── POLLING ──
            item { Spacer(Modifier.height(4.dp)); SectionHeader("POLLING") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Speed, contentDescription = null, tint = IceBlueGlow, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Poll Interval", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("${pollIntervalMs}ms between requests", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = pollIntervalMs.toFloat(),
                            onValueChange = {
                                pollIntervalMs = it.toInt()
                                obdViewModel.setPollIntervalMs(it.toInt())
                            },
                            valueRange = 50f..500f,
                            steps = 17,
                            colors = SliderDefaults.colors(
                                thumbColor = IceBlueGlow,
                                activeTrackColor = IceBlueGlow,
                                inactiveTrackColor = SurfaceElevated
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("50ms (fast)", color = TextMuted, fontSize = 10.sp)
                            Text("500ms (slow)", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }



            item {
                SettingToggleRow(
                    icon = Icons.Filled.Science,
                    title = "Experimental Ford PIDs",
                    subtitle = "Poll favourited Mode 22 DIDs. Equations are not yet validated by the supplied Torque export.",
                    checked = experimentalPids,
                    onCheckedChange = {
                        experimentalPids = it
                        obdViewModel.setExperimentalPidsEnabled(it)
                    }
                )
            }

            // ── DISPLAY ──
            item { Spacer(Modifier.height(4.dp)); SectionHeader("DISPLAY") }

            item {
                SettingInfoRow(
                    icon = Icons.Filled.Straighten,
                    title = "Units",
                    subtitle = "Metric / SI (°C, kPa, km/h). The inactive imperial selector was removed until every screen can convert consistently."
                )
            }

            // ── VEHICLE ──
            item { Spacer(Modifier.height(4.dp)); SectionHeader("VEHICLE") }

            item {
                SettingInfoRow(
                    icon = Icons.Filled.DirectionsCar,
                    title = "Vehicle Profile",
                    subtitle = "2018 Ford Ranger 3.2L P5AT • ISO 15765-4 CAN 11/500 • ECM 7E8 • TCM 7E9"
                )
            }

            // ── ABOUT ──
            item { Spacer(Modifier.height(4.dp)); SectionHeader("ABOUT") }

            item {
                SettingRow(
                    icon = Icons.Filled.Info,
                    title = "About Ford Guardian",
                    subtitle = "Version ${BuildConfig.VERSION_NAME} • Build ${BuildConfig.VERSION_CODE}",
                    onClick = { showAboutDialog = true }
                )
            }

            item {
                SettingRow(
                    icon = Icons.Filled.OpenInNew,
                    title = "Bluetooth Settings",
                    subtitle = "Open Android Bluetooth settings",
                    onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                )
            }
        }
    }

    // Device picker dialog
    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = { showDevicePicker = false },
            containerColor = SurfaceCard,
            title = { Text("Select OBD Adapter", color = TextPrimary) },
            text = {
                if (pairedDevices.isEmpty()) {
                    Text("No paired Bluetooth devices found. Pair your OBD adapter in Android Bluetooth settings first.", color = TextSecondary, fontSize = 13.sp)
                } else {
                    Column {
                        pairedDevices.forEach { device ->
                            val name = try { device.name ?: "Unknown" } catch (e: SecurityException) { device.address }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        preferredDeviceAddress = device.address
                                        prefs.edit().putString("preferred_device", device.address).apply()
                                        showDevicePicker = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = IceBlueGlow, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(name, color = TextPrimary, fontSize = 14.sp)
                                    Text(device.address, color = TextMuted, fontSize = 11.sp)
                                }
                                if (device.address == preferredDeviceAddress) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider(color = SurfaceElevated, thickness = 0.5.dp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDevicePicker = false }) { Text("Close", color = IceBlueGlow) }
            }
        )
    }

    // About dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Ford Guardian", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", color = TextSecondary, fontSize = 13.sp)
                    Text("OBD-II diagnostics configured for this 2018 Ford Ranger 3.2L diesel.", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Supports:", color = TextMuted, fontSize = 12.sp)
                    Text("\u2022 Opt-in Ford-specific PIDs (ECM 7E0, TCM 7E1)", color = TextMuted, fontSize = 12.sp)
                    Text("\u2022 Mode 01 Universal OBD-II PIDs", color = TextMuted, fontSize = 12.sp)
                    Text("\u2022 Real-time dashboard & parameter monitoring", color = TextMuted, fontSize = 12.sp)
                    Text("\u2022 Configurable threshold alerts", color = TextMuted, fontSize = 12.sp)
                    Text("\u2022 AI-powered diagnostics chat", color = TextMuted, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK", color = IceBlueGlow) }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = IceBlueGlow, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingInfoRow(icon: ImageVector, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = IceBlueGlow, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = IceBlueGlow, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = IceBlueGlow,
                    checkedTrackColor = IceBlueGlow.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceElevated
                )
            )
        }
    }
}
