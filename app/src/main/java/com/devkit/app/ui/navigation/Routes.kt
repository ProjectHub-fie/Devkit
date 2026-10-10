package com.devkit.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val TOOLS = "tools"
    const val API = "api"
    const val SAVED = "saved"
    const val SETTINGS = "settings"
    const val TOOL = "tool/{toolId}"

    fun tool(id: String) = "tool/$id"
}

enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home),
    TOOLS(Routes.TOOLS, "Tools", Icons.Filled.Widgets),
    API(Routes.API, "API", Icons.Filled.Cloud),
    SAVED(Routes.SAVED, "Saved", Icons.Filled.Bookmarks),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
}
