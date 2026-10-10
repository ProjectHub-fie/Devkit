package com.devkit.app.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devkit.app.DevKitApplication
import com.devkit.app.data.prefs.ThemeMode
import com.devkit.app.data.prefs.UserSettings
import com.devkit.app.ui.theme.DevKitTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as DevKitApplication
            val settings by app.settings.settings.collectAsStateWithLifecycle(
                initialValue = UserSettings()
            )
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DevKitTheme(
                darkTheme = darkTheme,
                accent = settings.accent,
                dynamicColor = settings.dynamicColor,
            ) {
                DevKitApp(
                    settings = settings,
                    onUpdateSettings = app.settings,
                    repository = app.repository,
                )
            }
        }
    }
}
