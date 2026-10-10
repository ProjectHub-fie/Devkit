package com.devkit.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devkit.app.BuildConfig
import com.devkit.app.data.prefs.AccentChoice
import com.devkit.app.data.prefs.HistoryRetention
import com.devkit.app.data.prefs.SettingsRepository
import com.devkit.app.data.prefs.ThemeMode
import com.devkit.app.data.prefs.UserSettings
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.SectionHeader
import com.devkit.app.ui.components.SettingClickRow
import com.devkit.app.ui.components.SettingSwitchRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    repository: DevKitRepository,
    settingsRepository: SettingsRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearHistory by remember { mutableStateOf(false) }
    var showClearAll by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { SectionHeader("Appearance") }
            item {
                Column {
                    Text("Theme", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { scope.launch { settingsRepository.setThemeMode(mode) } },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                }
            }
            item {
                Column {
                    Text("Accent color", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentChoice.entries.forEach { accent ->
                            AccentSwatch(
                                accent = accent,
                                selected = settings.accent == accent,
                                onClick = { scope.launch { settingsRepository.setAccent(accent) } },
                            )
                        }
                    }
                }
            }
            item {
                SettingSwitchRow(
                    title = "Dynamic color",
                    subtitle = "Use the system palette on Android 12 and newer",
                    checked = settings.dynamicColor,
                    onCheckedChange = { scope.launch { settingsRepository.setDynamicColor(it) } },
                )
            }

            item { SectionHeader("Tools") }
            item {
                Column {
                    Text("JSON indentation", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 4, 8).forEach { indent ->
                            FilterChip(
                                selected = settings.jsonIndent == indent,
                                onClick = { scope.launch { settingsRepository.setJsonIndent(indent) } },
                                label = { Text("$indent spaces") },
                            )
                        }
                    }
                }
            }
            item {
                Column {
                    Text("Default API timeout", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 15, 30, 60).forEach { seconds ->
                            FilterChip(
                                selected = settings.defaultTimeoutSeconds == seconds,
                                onClick = { scope.launch { settingsRepository.setDefaultTimeout(seconds) } },
                                label = { Text("${seconds}s") },
                            )
                        }
                    }
                }
            }
            item { SectionHeader("Privacy & API safety") }
            item {
                SettingSwitchRow(
                    title = "Warn before sending sensitive headers",
                    subtitle = "Shows a confirmation when Authorization or Cookie headers are about to be sent",
                    checked = settings.warnOnCrossOriginSecrets,
                    onCheckedChange = { scope.launch { settingsRepository.setWarnOnCrossOriginSecrets(it) } },
                )
            }
            item {
                SettingSwitchRow(
                    title = "Follow redirects",
                    subtitle = "Redirects are followed only for the same client; secrets are still redacted in history",
                    checked = settings.followRedirects,
                    onCheckedChange = { scope.launch { settingsRepository.setFollowRedirects(it) } },
                )
            }
            item { SectionHeader("Data") }
            item {
                Column {
                    Text("History retention", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HistoryRetention.entries.forEach { retention ->
                            FilterChip(
                                selected = settings.historyRetention == retention,
                                onClick = { scope.launch { settingsRepository.setHistoryRetention(retention) } },
                                label = { Text(retention.label) },
                            )
                        }
                    }
                }
            }
            item {
                SettingClickRow(
                    title = "Clear tool history",
                    subtitle = "Removes recent tool usage records",
                    onClick = { showClearHistory = true },
                )
            }
            item {
                SettingClickRow(
                    title = "Clear all local data",
                    subtitle = "Removes history, favorites, snippets, saved requests and environments",
                    onClick = { showClearAll = true },
                )
            }

            item { SectionHeader("About") }
            item {
                SettingClickRow(
                    title = "Dev Kit",
                    subtitle = "Developer tools, anywhere.",
                    value = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = {},
                )
            }
            item {
                SettingClickRow(
                    title = "Privacy policy",
                    subtitle = "How Dev Kit handles your data",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("${BuildConfig.PRIVACY_URL}"))
                        )
                    },
                )
            }
            item {
                SettingClickRow(
                    title = "Send feedback",
                    subtitle = "Open an issue on GitHub",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("${BuildConfig.FEEDBACK_URL}"))
                        )
                    },
                )
            }
            item {
                Text(
                    "Dev Kit processes tools on your device. Only the API Tester sends data, and only to the server you enter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }

    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            title = { Text("Clear tool history?") },
            text = { Text("Recent tools will be cleared. Favorites are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.clearToolHistory() }
                    showClearHistory = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearHistory = false }) { Text("Cancel") } },
        )
    }
    if (showClearAll) {
        AlertDialog(
            onDismissRequest = { showClearAll = false },
            title = { Text("Clear all local data?") },
            text = { Text("This permanently removes history, favorites, snippets, saved requests and environments from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.clearAllData() }
                    showClearAll = false
                }) { Text("Delete everything") }
            },
            dismissButton = { TextButton(onClick = { showClearAll = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AccentSwatch(accent: AccentChoice, selected: Boolean, onClick: () -> Unit) {
    val color = when (accent) {
        AccentChoice.VIOLET -> com.devkit.app.ui.theme.Violet500
        AccentChoice.BLUE -> com.devkit.app.ui.theme.AccentBlue
        AccentChoice.TEAL -> com.devkit.app.ui.theme.AccentTeal
        AccentChoice.AMBER -> com.devkit.app.ui.theme.AccentAmber
        AccentChoice.ROSE -> com.devkit.app.ui.theme.AccentRose
        AccentChoice.GREEN -> com.devkit.app.ui.theme.AccentGreen
    }
    Surface(
        color = color,
        shape = CircleShape,
        modifier = Modifier.width(36.dp).padding(2.dp),
        onClick = onClick,
        border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onBackground) else null,
    ) {}
}
