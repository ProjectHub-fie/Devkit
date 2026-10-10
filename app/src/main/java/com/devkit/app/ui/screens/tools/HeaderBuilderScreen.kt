package com.devkit.app.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.OutputViewer

/**
 * User-Agent / header builder. Produces a plausible request header line from
 * device facts plus a chosen browsing profile so API testers can format
 * requests realistically. It does not perform any network call.
 */
@Composable
fun HeaderBuilderScreen(repository: DevKitRepository, onBack: () -> Unit) {
    var profile by rememberSaveable { mutableStateOf("Chrome on Android") }
    val profiles = listOf(
        "Chrome on Android" to "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36",
        "Safari on iOS" to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
        "Desktop Chrome" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36",
        "curl" to "curl/8.6.0",
        "Dev Kit" to "DevKit/1.0 (Android)",
    )
    var output by rememberSaveable { mutableStateOf("") }

    ToolScaffoldWrapper("User-Agent Builder", "Pick a request profile and copy the header", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            profiles.forEach { (label, _) ->
                FilterChip(
                    selected = profile == label,
                    onClick = { profile = label },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(onClick = {
                val value = profiles.first { it.first == profile }.second
                output = "User-Agent: $value"
            }) { Text("Build header") }
            if (output.isNotEmpty()) OutputViewer(output, label = "Header")
            Text(
                "Some servers block unknown agents. Use these values only for testing systems you are allowed to test.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
