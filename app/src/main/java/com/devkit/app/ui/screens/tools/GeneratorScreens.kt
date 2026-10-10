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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devkit.app.core.ToolResult
import com.devkit.app.core.generators.GeneratorTools
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.OutputViewer
import com.devkit.app.ui.components.StatusBanner
import com.devkit.app.util.compute
import kotlinx.coroutines.launch

@Composable
fun UuidScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var version by rememberSaveable { mutableStateOf(GeneratorTools.UuidVersion.V4_RANDOM) }
    var count by rememberSaveable { mutableStateOf(5) }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ToolScaffoldWrapper("UUID Generator", "Random (v4) and time-ordered (v7) identifiers", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = version == GeneratorTools.UuidVersion.V4_RANDOM,
                    onClick = { version = GeneratorTools.UuidVersion.V4_RANDOM },
                    label = { Text("v4 random") },
                )
                FilterChip(
                    selected = version == GeneratorTools.UuidVersion.V7_TIME_ORDERED,
                    onClick = { version = GeneratorTools.UuidVersion.V7_TIME_ORDERED },
                    label = { Text("v7 time-ordered") },
                )
            }
            Text("Count: $count", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = count.toFloat(),
                onValueChange = { count = it.toInt().coerceIn(1, 100) },
                valueRange = 1f..100f,
                steps = 98,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    when (val r = GeneratorTools.uuids(count, version)) {
                        is ToolResult.Success -> { output = r.value.joinToString("\n"); error = null }
                        is ToolResult.Failure -> { output = ""; error = r.message }
                    }
                }) { Text("Generate") }
                OutlinedButton(onClick = {
                    when (val r = GeneratorTools.uuids(1, version)) {
                        is ToolResult.Success -> output = r.value.first()
                        is ToolResult.Failure -> error = r.message
                    }
                }) { Text("Single") }
            }
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) OutputViewer(output, label = "UUIDs", maxLines = 30)
        }
    }
}

@Composable
fun RandomStringScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var length by rememberSaveable { mutableStateOf(24) }
    var lower by rememberSaveable { mutableStateOf(true) }
    var upper by rememberSaveable { mutableStateOf(true) }
    var digits by rememberSaveable { mutableStateOf(true) }
    var symbols by rememberSaveable { mutableStateOf(false) }
    var ambiguous by rememberSaveable { mutableStateOf(true) }
    var passwordMode by rememberSaveable { mutableStateOf(false) }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var strength by rememberSaveable { mutableStateOf<String?>(null) }

    ToolScaffoldWrapper("Random String / Password", "Cryptographically secure randomness", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !passwordMode, onClick = { passwordMode = false }, label = { Text("Random string") })
                FilterChip(selected = passwordMode, onClick = { passwordMode = true }, label = { Text("Password") })
            }
            Text("Length: $length", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = length.toFloat(),
                onValueChange = { length = it.toInt().coerceIn(4, 128) },
                valueRange = 4f..128f,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = lower, onClick = { lower = !lower }, label = { Text("a-z") })
                FilterChip(selected = upper, onClick = { upper = !upper }, label = { Text("A-Z") })
                FilterChip(selected = digits, onClick = { digits = !digits }, label = { Text("0-9") })
                FilterChip(selected = symbols, onClick = { symbols = !symbols }, label = { Text("!@#") })
            }
            if (!passwordMode) {
                FilterChip(
                    selected = ambiguous,
                    onClick = { ambiguous = !ambiguous },
                    label = { Text("Exclude look-alike characters (Il1O0o)") },
                )
            }
            Text(
                "A random string is not automatically a strong password. Enable all character sets and a longer length for passwords.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                scope.launch {
                    val result = compute {
                        if (passwordMode) {
                            GeneratorTools.password(length, lower, upper, digits, symbols)
                        } else {
                            val alphabet = buildString {
                                if (lower) append(GeneratorTools.LOWER)
                                if (upper) append(GeneratorTools.UPPER)
                                if (digits) append(GeneratorTools.DIGITS)
                                if (symbols) append(GeneratorTools.SYMBOLS)
                            }
                            GeneratorTools.randomString(length, alphabet, ambiguous)
                        }
                    }
                    when (result) {
                        is ToolResult.Success -> {
                            output = result.value
                            strength = GeneratorTools.passwordStrength(result.value).let {
                                "${it.label} — about ${it.entropyBits.toInt()} bits of entropy"
                            }
                            error = null
                        }
                        is ToolResult.Failure -> { output = ""; error = result.message; strength = null }
                    }
                }
            }) { Text(if (passwordMode) "Generate password" else "Generate string") }
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) {
                OutputViewer(output, label = "Result", maxLines = 6)
                strength?.let { StatusBanner(it, isError = false) }
            }
        }
    }
}

@Composable
fun HashGeneratorScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("Dev Kit") }
    var key by rememberSaveable { mutableStateOf("") }
    var hmacMode by rememberSaveable { mutableStateOf(false) }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ToolScaffoldWrapper("Hash Generator", "MD5, SHA-1, SHA-256, SHA-384, SHA-512 and HMAC", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilterChip(selected = hmacMode, onClick = { hmacMode = !hmacMode }, label = { Text("HMAC mode") })
            CodeEditor(value = input, onValueChange = { input = it }, label = "Input text", minLines = 5)
            if (hmacMode) {
                CodeEditor(value = key, onValueChange = { key = it }, label = "HMAC key", minLines = 2, maxLines = 3)
            }
            Text(
                "A hash is a one-way digest, not encryption. MD5 and SHA-1 are weak for security use.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                scope.launch {
                    val lines = mutableListOf<String>()
                    var failure: String? = null
                    compute {
                        GeneratorTools.HashAlgorithm.entries.forEach { algorithm ->
                            val result = if (hmacMode) {
                                GeneratorTools.hmac(input, key, algorithm)
                            } else {
                                GeneratorTools.hash(input, algorithm)
                            }
                            when (result) {
                                is ToolResult.Success -> lines.add("${algorithm.name.padEnd(8)} ${result.value}")
                                is ToolResult.Failure -> failure = result.message
                            }
                        }
                    }
                    output = lines.joinToString("\n")
                    error = failure
                }
            }) { Text("Compute hashes") }
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) OutputViewer(output, label = "Digests", maxLines = 10)
        }
    }
}
