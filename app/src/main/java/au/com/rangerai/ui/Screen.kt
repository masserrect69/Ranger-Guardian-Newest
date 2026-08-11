package au.com.rangerai.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Speed)
    object Parameters : Screen("parameters", "Params", Icons.Filled.Dashboard)
    object Alerts : Screen("alerts", "Alerts", Icons.Filled.Warning)
    object History : Screen("history", "History", Icons.Filled.Timeline)
    object Reports : Screen("reports", "Reports", Icons.Filled.Assessment)
    object Chat : Screen("chat", "AI Chat", Icons.Filled.SmartToy)
    object Maintenance : Screen("maintenance", "Service", Icons.Filled.Build)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
    object About : Screen("about", "About", Icons.Filled.Info)
    object VehicleSelect : Screen("vehicle_select", "Vehicle", Icons.Filled.DirectionsCar)
}
