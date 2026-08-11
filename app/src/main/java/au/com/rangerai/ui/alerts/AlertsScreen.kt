package au.com.rangerai.ui.alerts

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.rangerai.bluetooth.ObdViewModel
import au.com.rangerai.data.DiagnosticTroubleCode
import au.com.rangerai.ui.theme.*

data class AlertRule(
    val pidName: String,
    val pidDescription: String,
    val unit: String,
    val warningThreshold: Double,
    val criticalThreshold: Double,
    val isEnabled: Boolean = true,
    val isHighAlert: Boolean = true,
    val isVerified: Boolean = true
)

@Composable
fun AlertsScreen(obdViewModel: ObdViewModel = viewModel()) {
    val vehicleState by obdViewModel.vehicleState.collectAsState()
    val dtcs by obdViewModel.diagnosticTroubleCodes.collectAsState()
    val isReadingDtcs by obdViewModel.isReadingDtcs.collectAsState()
    val isConnected by obdViewModel.isConnected.collectAsState()

    val defaultAlerts = remember {
        listOf(
            // SAE Mode 01 formula and source are verified for this vehicle.
            AlertRule("ECT_01", "Engine Coolant Temperature", "°C", 103.0, 110.0, true, true, true),

            // Ford Mode 22 rules are disabled by default because their equations
            // were not present in the supplied Torque custom-PID export.
            AlertRule("EOT", "Engine Oil Temperature", "°C", 115.0, 130.0, false, true, false),
            AlertRule("DPF_SOOT_RANGER", "DPF Soot Load", "%", 70.0, 85.0, false, true, false),
            AlertRule("DPF_PRESS_DIF", "DPF Differential Pressure", "kPa", 15.0, 25.0, false, true, false),
            AlertRule("EGT_PRE_RANGER", "Pre-Turbo Exhaust Temperature", "°C", 700.0, 800.0, false, true, false),
            AlertRule("TFT", "Transmission Fluid Temperature", "°C", 110.0, 125.0, false, true, false)
        )
    }

    var alertRules by remember { mutableStateOf(defaultAlerts) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingAlert by remember { mutableStateOf<AlertRule?>(null) }

    val activeAlerts = remember(vehicleState, alertRules) {
        alertRules.filter { rule ->
            if (!rule.isEnabled) return@filter false
            val value = vehicleState.freshValue(rule.pidName) ?: return@filter false
            if (rule.isHighAlert) value >= rule.warningThreshold else value <= rule.warningThreshold
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = IceBlueGlow, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Alerts", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${activeAlerts.size} active \u2022 ${alertRules.count { it.isEnabled }} rules enabled",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TROUBLE CODES", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                    TextButton(
                        enabled = isConnected && !isReadingDtcs,
                        onClick = obdViewModel::refreshDiagnosticTroubleCodes
                    ) {
                        if (isReadingDtcs) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = IceBlueGlow)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (dtcs.isEmpty()) "Scan" else "Rescan", color = if (isConnected) IceBlueGlow else TextMuted, fontSize = 12.sp)
                    }
                }
            }

            if (dtcs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Text(
                            if (isConnected) "Tap Scan to read stored, pending and permanent OBD trouble codes from ECM 7E8 and TCM 7E9."
                            else "Connect the vLinker FS to scan trouble codes.",
                            modifier = Modifier.padding(14.dp),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                items(dtcs, key = { "${it.responseHeader}_${it.status}_${it.code}" }) { dtc ->
                    DtcCard(dtc)
                }
            }

            item {
                Text(
                    "Only fresh values can trigger alerts. Ford Mode 22 rules are experimental and disabled by default; DPF pressure and EGT must be interpreted with RPM, load and regeneration state.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(4.dp))
            }

            if (activeAlerts.isNotEmpty()) {
                item {
                    Text("ACTIVE ALERTS", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                }
                items(activeAlerts) { rule ->
                    val value = vehicleState.freshValue(rule.pidName)
                    val isCritical = value != null && (
                        if (rule.isHighAlert) value >= rule.criticalThreshold else value <= rule.criticalThreshold
                    )
                    ActiveAlertCard(rule, value, isCritical)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ALERT RULES", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { alertRules = defaultAlerts }) {
                        Text("Reset", color = TextMuted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            items(alertRules) { rule ->
                val currentValue = vehicleState.freshValue(rule.pidName)
                AlertRuleCard(
                    rule = rule,
                    currentValue = currentValue,
                    onToggle = { enabled ->
                        alertRules = alertRules.map { if (it.pidName == rule.pidName) it.copy(isEnabled = enabled) else it }
                    },
                    onEdit = { editingAlert = rule; showEditDialog = true }
                )
            }
        }
    }

    if (showEditDialog && editingAlert != null) {
        val rule = editingAlert!!
        var warningText by remember { mutableStateOf(rule.warningThreshold.toString()) }
        var criticalText by remember { mutableStateOf(rule.criticalThreshold.toString()) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Edit: ${rule.pidDescription}", color = TextPrimary, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Set thresholds for ${rule.pidDescription} (${rule.unit})", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = warningText,
                        onValueChange = { warningText = it },
                        label = { Text("Warning threshold (${rule.unit})", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentYellow,
                            unfocusedBorderColor = SurfaceElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = criticalText,
                        onValueChange = { criticalText = it },
                        label = { Text("Critical threshold (${rule.unit})", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentRed,
                            unfocusedBorderColor = SurfaceElevated,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val warning = warningText.toDoubleOrNull() ?: rule.warningThreshold
                    val critical = criticalText.toDoubleOrNull() ?: rule.criticalThreshold
                    alertRules = alertRules.map {
                        if (it.pidName == rule.pidName) it.copy(warningThreshold = warning, criticalThreshold = critical) else it
                    }
                    showEditDialog = false
                }) { Text("Save", color = IceBlueGlow) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

@Composable
private fun DtcCard(dtc: DiagnosticTroubleCode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BugReport, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(dtc.code, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${dtc.status.label} • ${dtc.responseHeader ?: "Unknown ECU"}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ActiveAlertCard(rule: AlertRule, value: Double?, isCritical: Boolean) {
    val color = if (isCritical) AccentRed else AccentYellow
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isCritical) Icons.Filled.Error else Icons.Filled.Warning,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.pidDescription, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isCritical) "CRITICAL: ${value?.let { "%.1f".format(it) } ?: "--"} ${rule.unit} (limit: ${rule.criticalThreshold})"
                    else "WARNING: ${value?.let { "%.1f".format(it) } ?: "--"} ${rule.unit} (limit: ${rule.warningThreshold})",
                    color = color.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AlertRuleCard(
    rule: AlertRule,
    currentValue: Double?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    val isTriggered = currentValue != null && (
        if (rule.isHighAlert) currentValue >= rule.warningThreshold else currentValue <= rule.warningThreshold
    )
    val statusColor = when {
        !rule.isEnabled -> TextMuted
        isTriggered -> AccentYellow
        else -> AccentGreen
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(statusColor, shape = RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.pidDescription, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (rule.isVerified) "VERIFIED" else "EXPERIMENTAL",
                        color = if (rule.isVerified) AccentGreen else AccentOrange,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Warn: ${rule.warningThreshold} ${rule.unit}", color = AccentYellow.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text("Crit: ${rule.criticalThreshold} ${rule.unit}", color = AccentRed.copy(alpha = 0.8f), fontSize = 11.sp)
                    if (currentValue != null) Text("Now: ${"%.1f".format(currentValue)}", color = TextMuted, fontSize = 11.sp)
                }
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = IceBlueGlow,
                    checkedTrackColor = IceBlueGlow.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = SurfaceElevated
                ),
                modifier = Modifier.height(24.dp)
            )
        }
    }
}
