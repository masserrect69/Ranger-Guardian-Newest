package au.com.rangerai.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import au.com.rangerai.bluetooth.ObdViewModel
import au.com.rangerai.ui.about.AboutScreen
import au.com.rangerai.ui.alerts.AlertsScreen
import au.com.rangerai.ui.chat.ChatScreen
import au.com.rangerai.ui.dashboard.DashboardScreen
import au.com.rangerai.ui.history.HistoryScreen
import au.com.rangerai.ui.maintenance.MaintenanceScreen
import au.com.rangerai.ui.parameters.ParametersScreen
import au.com.rangerai.ui.reports.ReportsScreen
import au.com.rangerai.ui.settings.SettingsScreen
import au.com.rangerai.ui.theme.SurfaceCard
import au.com.rangerai.ui.vehicle.VehicleSelectScreen

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Parameters,
    Screen.Alerts,
    Screen.Chat,
    Screen.Settings
)

@Composable
fun FordGuardianNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val obdViewModel: ObdViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = SurfaceCard) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(obdViewModel)
            }
            composable(Screen.Parameters.route) {
                ParametersScreen(obdViewModel)
            }
            composable(Screen.Alerts.route) {
                AlertsScreen(obdViewModel)
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Reports.route) {
                ReportsScreen()
            }
            composable(Screen.Chat.route) {
                ChatScreen(obdViewModel)
            }
            composable(Screen.Maintenance.route) {
                MaintenanceScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(navController, obdViewModel)
            }
            composable(Screen.About.route) {
                AboutScreen()
            }
            composable(Screen.VehicleSelect.route) {
                VehicleSelectScreen { manufacturer ->
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
}
