package au.com.rangerai.ui.parameters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.rangerai.bluetooth.ObdViewModel
import au.com.rangerai.data.PidDefinition
import au.com.rangerai.data.PidPriority
import au.com.rangerai.ui.theme.*

/** Uses the same authoritative registry as the poller and AI context. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParametersScreen(obdViewModel: ObdViewModel = viewModel()) {
    val allPids = remember(obdViewModel) { obdViewModel.pidRegistry.allPids.distinctBy { it.key } }
    val availableCategories = remember(allPids) {
        allPids.map { it.category.displayName }.distinct().sorted()
    }
    val favourites by obdViewModel.favourites.collectAsState()
    val vehicleState by obdViewModel.vehicleState.collectAsState()
    val supportedByEcu by obdViewModel.supportedMode01Pids.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showFavouritesOnly by remember { mutableStateOf(false) }

    val filteredPids = remember(searchQuery, selectedCategory, showFavouritesOnly, favourites, allPids) {
        allPids.filter { pid ->
            val matchesSearch = searchQuery.isBlank() ||
                pid.name.contains(searchQuery, ignoreCase = true) ||
                pid.description.contains(searchQuery, ignoreCase = true) ||
                pid.did.contains(searchQuery, ignoreCase = true) ||
                pid.unit.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || pid.category.displayName == selectedCategory
            val matchesFavourite = !showFavouritesOnly || pid.key in favourites
            matchesSearch && matchesCategory && matchesFavourite
        }
    }

    Column(Modifier.fillMaxSize().background(SurfaceBlack)) {
        Surface(color = SurfaceCard, shadowElevation = 4.dp) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Dashboard, null, tint = IceBluePrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Parameters", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (supportedByEcu.isEmpty()) "Waiting for ECU capability scan"
                            else "ECM ${supportedByEcu["7E8"]?.size ?: 0} • TCM ${supportedByEcu["7E9"]?.size ?: 0} standard PIDs",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    FilterChip(
                        selected = showFavouritesOnly,
                        onClick = { showFavouritesOnly = !showFavouritesOnly },
                        label = { Text("★ ${favourites.size}", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentOrange.copy(alpha = 0.2f),
                            selectedLabelColor = AccentOrange,
                            containerColor = SurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = null
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search ${allPids.size} parameters…", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, "Clear", tint = TextMuted)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBluePrimary,
                        unfocusedBorderColor = TextMuted,
                        cursorColor = IceBluePrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryChip("All (${allPids.size})", selectedCategory == null) { selectedCategory = null }
            }
            items(availableCategories) { category ->
                val count = allPids.count { it.category.displayName == category }
                CategoryChip("$category ($count)", selectedCategory == category) {
                    selectedCategory = if (selectedCategory == category) null else category
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${filteredPids.size} parameters", color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("Mode 22 = experimental", color = TextMuted, fontSize = 11.sp)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredPids, key = { it.key }) { pid ->
                val value = vehicleState.freshValue(pid.name)
                ParameterCard(
                    pid = pid,
                    value = value,
                    isFavourite = pid.key in favourites,
                    onToggleFavourite = { obdViewModel.toggleFavourite(pid.key) }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, tint = IceBlueDim, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Verified Mode 01 sensors are polled automatically. Favourited Ford Mode 22 DIDs are polled only when Experimental Ford PIDs is enabled in Settings.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = IceBluePrimary.copy(alpha = 0.2f),
            selectedLabelColor = IceBluePrimary,
            containerColor = SurfaceCard,
            labelColor = TextSecondary
        ),
        border = null
    )
}

@Composable
private fun ParameterCard(
    pid: PidDefinition,
    value: Double?,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit
) {
    val verified = pid.obdMode == 1
    val priorityLabel = when (pid.priority) {
        PidPriority.HIGH -> "Fast"
        PidPriority.MEDIUM -> "Medium"
        PidPriority.LOW -> "Slow"
    }
    val priorityColor = when (pid.priority) {
        PidPriority.HIGH -> AccentGreen
        PidPriority.MEDIUM -> AccentYellow
        PidPriority.LOW -> TextMuted
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isFavourite) SurfaceCard else SurfaceGauge),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(
                    if (verified) IceBluePrimary else AccentOrange
                )
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pid.description, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Surface(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp)) {
                        Text(priorityLabel, Modifier.padding(horizontal = 4.dp, vertical = 1.dp), color = priorityColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "${if (verified) "Mode 01 verified" else "Mode 22 experimental"} • ${pid.ecuHeader ?: "7E0"} • ${pid.did}",
                    color = if (verified) TextMuted else AccentOrange.copy(alpha = 0.9f),
                    fontSize = 10.sp
                )
                Text(pid.category.displayName, color = TextSecondary, fontSize = 10.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    value?.let(::formatValue) ?: "--",
                    color = if (value != null) IceBlueGlow else TextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(pid.unit, color = TextMuted, fontSize = 10.sp)
            }

            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onToggleFavourite, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isFavourite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    if (isFavourite) "Remove from dashboard" else "Add to dashboard",
                    tint = if (isFavourite) AccentOrange else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun formatValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
