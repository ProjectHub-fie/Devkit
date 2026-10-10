package com.devkit.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.devkit.app.core.ToolCatalog
import com.devkit.app.data.prefs.SettingsRepository
import com.devkit.app.data.prefs.UserSettings
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.navigation.Routes
import com.devkit.app.ui.navigation.TopDestination
import com.devkit.app.ui.screens.ApiTesterScreen
import com.devkit.app.ui.screens.HomeScreen
import com.devkit.app.ui.screens.SavedScreen
import com.devkit.app.ui.screens.SettingsScreen
import com.devkit.app.ui.screens.ToolsScreen
import com.devkit.app.ui.screens.tools.ToolHost

private const val RAIL_BREAKPOINT_DP = 700

@Composable
fun DevKitApp(
    settings: UserSettings,
    onUpdateSettings: SettingsRepository,
    repository: DevKitRepository,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(settings.historyRetention) {
        repository.pruneHistory(settings.historyRetention.days)
    }

    val topRoutes = TopDestination.entries.map { it.route }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= RAIL_BREAKPOINT_DP.dp

        val navigateTo: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        if (useRail) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    TopDestination.entries.forEach { dest ->
                        NavigationRailItem(
                            selected = currentRoute == dest.route,
                            onClick = { navigateTo(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    DevKitNavHost(navController, settings, onUpdateSettings, repository)
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    if (currentRoute in topRoutes) {
                        NavigationBar {
                            TopDestination.entries.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateTo(dest.route) },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    DevKitNavHost(navController, settings, onUpdateSettings, repository)
                }
            }
        }
    }
}

@Composable
private fun DevKitNavHost(
    navController: androidx.navigation.NavHostController,
    settings: UserSettings,
    onUpdateSettings: SettingsRepository,
    repository: DevKitRepository,
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                repository = repository,
                settings = settings,
                onOpenTool = { navController.navigate(Routes.tool(it)) },
                onOpenApiTester = { navController.navigate(Routes.API) },
                onOpenCategory = { navController.navigate(Routes.TOOLS) },
            )
        }
        composable(Routes.TOOLS) {
            ToolsScreen(
                repository = repository,
                onOpenTool = { navController.navigate(Routes.tool(it)) },
            )
        }
        composable(Routes.API) {
            ApiTesterScreen(repository = repository, settings = settings)
        }
        composable(Routes.SAVED) {
            SavedScreen(
                repository = repository,
                onOpenTool = { navController.navigate(Routes.tool(it)) },
                onOpenApiTester = { navController.navigate(Routes.API) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                repository = repository,
                settingsRepository = onUpdateSettings,
            )
        }
        composable(
            route = Routes.TOOL,
            arguments = listOf(navArgument("toolId") { type = NavType.StringType }),
        ) { entry ->
            val toolId = entry.arguments?.getString("toolId").orEmpty()
            val tool = ToolCatalog.byId(toolId)
            ToolHost(
                toolId = toolId,
                repository = repository,
                settings = settings,
                onBack = { navController.popBackStack() },
                onOpenApiTester = { navController.navigate(Routes.API) },
            )
        }
    }
}
