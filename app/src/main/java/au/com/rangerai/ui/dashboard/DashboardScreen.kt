package au.com.rangerai.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.rangerai.bluetooth.ObdConnection
import au.com.rangerai.bluetooth.ObdViewModel
import au.com.rangerai.data.PidDefinition
import au.com.rangerai.data.VehicleState
import au.com.rangerai.ui.theme.*

@Composable
fun DashboardScreen(obdViewModel: ObdViewModel = viewModel()) {
    val connectionState by obdViewModel.connectionState.collectAsState()
    val vehicleState by obdViewModel.vehicleState.collectAsState()
    val uiError by obdViewModel.uiError.collectAsState()
    val favourites by obdViewModel.favourites.collectAsState()
    val deviceName by obdViewModel.connectedDeviceName.collectAsState()
    val isConnected = connectionState is ObdConnection.ConnectionState.Connected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 12.dp)
    ) {
        ConnectionStatusBar(
            isConnected = isConnected,
            deviceName = deviceName,
            paramCount = vehicleState.freshParameters().size
        )

        if (uiError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(uiError ?: "", color = AccentRed, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { obdViewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = AccentRed)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (isConnected) {
            ConnectedDashboard(vehicleState, favourites, obdViewModel)
        } else {
            DisconnectedView(connectionState, obdViewModel)
        }
    }
}

@Composable
private fun ConnectionStatusBar(isConnected: Boolean, deviceName: String?, paramCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = if (isConnected) IceBlueGlow else TextMuted
        val label = if (isConnected) deviceName ?: "Connected" else "Disconnected"
        Icon(
            if (isConnected) Icons.Filled.CheckCircle else Icons.Filled.BluetoothSearching,
            contentDescription = null, tint = tint, modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (isConnected && paramCount > 0) {
            Spacer(Modifier.width(8.dp))
            Text("\u2022 $paramCount PIDs active", color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ConnectedDashboard(
    vehicleState: VehicleState,
    favourites: Set<String>,
    obdViewModel: ObdViewModel
) {
    val pidRegistry = remember(obdViewModel) { obdViewModel.pidRegistry }

    val pidToStateKey: Map<String, String> = remember(pidRegistry) {
        val map = linkedMapOf<String, String>()
        for (pid in pidRegistry.allPids) {
            val modePrefix = if (pid.obdMode == 1) "01" else "22"
            map["${modePrefix}_${pid.did}"] = pid.name
        }
        map
    }

    val liveCheck = remember(vehicleState) { calculateLiveReadingCheck(vehicleState) }

    val favouritePids: List<PidDefinition> = remember(favourites, pidRegistry) {
        pidRegistry.allPids.filter { pid: PidDefinition ->
            val key = "${if (pid.obdMode == 1) "01" else "22"}_${pid.did}"
            favourites.contains(key)
        }
    }

    val primaryGauges = favouritePids.take(4)
    val secondaryPids = if (favouritePids.size > 4) favouritePids.drop(4) else emptyList()
    val primaryRows = primaryGauges.chunked(2)
    val secondaryRows = secondaryPids.chunked(2)
    val debugInfo = obdViewModel.getDebugInfo()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            LiveReadingCheckCard(liveCheck)
            Spacer(Modifier.height(16.dp))
        }

        if (primaryGauges.isNotEmpty()) {
            item {
                Text("PRIMARY GAUGES", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
            }
            items(primaryRows) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { pid ->
                        val stateKey = "${if (pid.obdMode == 1) "01" else "22"}_${pid.did}"
                        val value = pidToStateKey[stateKey]?.let(vehicleState::freshValue)
                        PrimaryGaugeCard(pid, value, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        if (secondaryPids.isNotEmpty()) {
            item {
                Text("FAVOURITES", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
            }
            items(secondaryRows) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { pid ->
                        val stateKey = "${if (pid.obdMode == 1) "01" else "22"}_${pid.did}"
                        val value = pidToStateKey[stateKey]?.let(vehicleState::freshValue)
                        CompactPidCard(
                            pid = pid,
                            value = value,
                            isFavourite = true,
                            onToggleFavourite = { obdViewModel.toggleFavourite(stateKey) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if (favouritePids.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No favourites yet", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Go to Params tab and star PIDs to show them here", color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(debugInfo, color = TextMuted.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

private data class LiveReadingCheck(
    val score: Int?,
    val title: String,
    val detail: String
)

@Composable
private fun LiveReadingCheckCard(result: LiveReadingCheck) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                val score = result.score
                val color = when {
                    score == null -> TextMuted
                    score >= 90 -> IceBlueGlow
                    score >= 70 -> Color(0xFFFFBF00)
                    else -> AccentRed
                }
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = color.copy(alpha = 0.2f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 8f, cap = StrokeCap.Round))
                    if (score != null) {
                        drawArc(color = color, startAngle = -90f, sweepAngle = 360f * score / 100f, useCenter = false, style = Stroke(width = 8f, cap = StrokeCap.Round))
                    }
                }
                Text(score?.toString() ?: "--", color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Live Reading Check", color = TextSecondary, fontSize = 12.sp)
                Text(result.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(result.detail, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun PrimaryGaugeCard(pid: PidDefinition, value: Double?, modifier: Modifier = Modifier) {
    // Guard: show '--' if value is below the physical minimum (e.g. injectors when engine is off)
    val displayValue = value?.takeIf { it >= pid.minValue }
    Card(
        modifier = modifier.padding(4.dp).aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(pid.name, color = IceBlueGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            Text(displayValue?.let { "%.1f".format(it) } ?: "--", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(pid.unit, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CompactPidCard(
    pid: PidDefinition,
    value: Double?,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Guard: show '--' if value is below the physical minimum (e.g. injectors when engine is off)
    val displayValue = value?.takeIf { it >= pid.minValue }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pid.description, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${displayValue?.let { "%.1f".format(it) } ?: "--"} ${pid.unit}",
                    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onToggleFavourite, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Star, contentDescription = "Toggle favourite", tint = if (isFavourite) IceBlueGlow else TextMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun DisconnectedView(connectionState: ObdConnection.ConnectionState, obdViewModel: ObdViewModel) {
    val isConnecting = connectionState is ObdConnection.ConnectionState.Connecting ||
            connectionState is ObdConnection.ConnectionState.Initializing
    var showDeviceList by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "bt")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (isConnecting) Icons.Filled.BluetoothSearching else Icons.Filled.Bluetooth,
            contentDescription = null,
            tint = IceBlueGlow.copy(alpha = if (isConnecting) alpha else 0.6f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            when (connectionState) {
                is ObdConnection.ConnectionState.Connecting -> "Connecting..."
                is ObdConnection.ConnectionState.Initializing -> "Initializing adapter..."
                else -> "Not Connected"
            },
            color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Connect your OBD-II adapter to start monitoring",
            color = TextMuted, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(32.dp))

        if (!isConnecting) {
            Button(
                onClick = { obdViewModel.autoConnect() },
                modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IceBlueGlow, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Auto Connect", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showDeviceList = !showDeviceList },
                modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, IceBlueGlow.copy(alpha = 0.5f))
            ) {
                Text("Select Device", color = IceBlueGlow)
            }
        } else {
            CircularProgressIndicator(color = IceBlueGlow, modifier = Modifier.size(32.dp))
        }

        if (showDeviceList) {
            Spacer(Modifier.height(16.dp))
            val pairedDevices = remember { obdViewModel.getPairedDevices() }
            if (pairedDevices.isEmpty()) {
                Text("No paired devices found", color = TextMuted, fontSize = 13.sp)
            } else {
                // Scrollable list so all paired devices are reachable regardless of count
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .heightIn(max = 300.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(pairedDevices) { device ->
                        val name = try { device.name ?: "Unknown" } catch (e: SecurityException) { "Unknown" }
                        TextButton(
                            onClick = { showDeviceList = false; obdViewModel.connectToDevice(device) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.BluetoothSearching, contentDescription = null, tint = IceBlueGlow, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(name, color = TextPrimary, modifier = Modifier.weight(1f))
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    }
                }
            }
        }
    }
}

private fun calculateLiveReadingCheck(vehicleState: VehicleState): LiveReadingCheck {
    val rpm = vehicleState.freshValue("RPM_01")
    val coolant = vehicleState.freshValue("ECT_01")
    val voltage = vehicleState.freshValue("BATT_V_01")

    var score = 100
    var evaluated = 0
    val findings = mutableListOf<String>()

    coolant?.let { value ->
        evaluated++
        when {
            value >= 110.0 -> { score -= 40; findings += "coolant is critically high" }
            value >= 103.0 -> { score -= 22; findings += "coolant is high" }
            value >= 98.0 -> { score -= 8; findings += "coolant is warmer than expected" }
        }
    }

    voltage?.let { value ->
        evaluated++
        val engineRunning = (rpm ?: 0.0) > 400.0
        when {
            value > 15.5 -> { score -= 25; findings += "module voltage is unusually high" }
            engineRunning && value < 11.5 -> { score -= 30; findings += "module voltage is very low while running" }
            !engineRunning && value < 11.8 -> { score -= 25; findings += "battery voltage is very low" }
            !engineRunning && value < 12.1 -> { score -= 10; findings += "battery voltage is low" }
        }
    }

    if (evaluated == 0) {
        return LiveReadingCheck(null, "Collecting verified data", "No fresh safety-check readings yet. This is not a full vehicle diagnosis.")
    }

    val bounded = score.coerceIn(0, 100)
    val title = when {
        bounded >= 90 -> "Verified readings look normal"
        bounded >= 70 -> "A verified reading needs review"
        else -> "Attention recommended"
    }
    val detail = if (findings.isEmpty()) {
        "$evaluated verified checks evaluated. Experimental Ford PIDs are excluded."
    } else {
        findings.joinToString(prefix = "Check: ", separator = "; ") + "."
    }
    return LiveReadingCheck(bounded, title, detail)
}
