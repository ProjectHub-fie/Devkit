package com.devkit.app.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devkit.app.core.ToolResult
import com.devkit.app.core.encoding.EncodingTools
import com.devkit.app.core.text.DateTimeTools
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.ActionBar
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.OutputViewer
import com.devkit.app.ui.components.StatusBanner
import com.devkit.app.util.compute
import java.time.ZoneId
import kotlinx.coroutines.launch

@Composable
fun Base64Screen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("Dev Kit — offline tools") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var urlSafe by rememberSaveable { mutableStateOf(false) }
    var decode by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }

    ToolScaffoldWrapper("Base64 Encoder / Decoder", "UTF-8 text in both directions", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !decode, onClick = { decode = false }, label = { Text("Encode") })
                FilterChip(selected = decode, onClick = { decode = true }, label = { Text("Decode") })
                FilterChip(selected = urlSafe, onClick = { urlSafe = !urlSafe }, label = { Text("URL-safe") })
            }
            CodeEditor(
                value = input,
                onValueChange = { input = it },
                label = if (decode) "Base64 input" else "Plain text",
                minLines = 6,
            )
            ActionBar(
                primaryLabel = if (decode) "Decode" else "Encode",
                loading = busy,
                onPrimary = {
                    busy = true
                    scope.launch {
                        val result = compute {
                            if (decode) {
                                EncodingTools.base64Decode(input, urlSafe)
                            } else {
                                EncodingTools.base64Encode(input).let {
                                    when (it) {
                                        is ToolResult.Success ->
                                            ToolResult.Success(if (urlSafe) it.value.urlSafe else it.value.standard)
                                        is ToolResult.Failure -> it
                                    }
                                }
                            }
                        }
                        busy = false
                        when (result) {
                            is ToolResult.Success -> { output = result.value; error = null }
                            is ToolResult.Failure -> { output = ""; error = result.message }
                        }
                    }
                },
            )
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) OutputViewer(output, label = "Result")
        }
    }
}

@Composable
fun UrlCodecScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("search=dev kit & tools") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var fullUrl by rememberSaveable { mutableStateOf(false) }
    var decode by rememberSaveable { mutableStateOf(false) }

    ToolScaffoldWrapper("URL Encoder / Decoder", "Component vs full-URL encoding", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !decode, onClick = { decode = false }, label = { Text("Encode") })
                FilterChip(selected = decode, onClick = { decode = true }, label = { Text("Decode") })
                FilterChip(selected = fullUrl, onClick = { fullUrl = !fullUrl }, label = { Text("Keep URL structure") })
            }
            Text(
                if (fullUrl) "Keeps : / ? & = # so an entire URL stays readable."
                else "Encodes everything outside the unreserved set (use inside a query value).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CodeEditor(value = input, onValueChange = { input = it }, label = "Input", minLines = 5)
            ActionBar(
                primaryLabel = if (decode) "Decode" else "Encode",
                onPrimary = {
                    scope.launch {
                        val result = compute {
                            when {
                                decode -> EncodingTools.urlDecode(input)
                                fullUrl -> ToolResult.Success(EncodingTools.urlEncodeFull(input))
                                else -> ToolResult.Success(EncodingTools.urlEncodeComponent(input))
                            }
                        }
                        when (result) {
                            is ToolResult.Success -> { output = result.value; error = null }
                            is ToolResult.Failure -> { output = ""; error = result.message }
                        }
                    }
                },
            )
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) OutputViewer(output, label = "Result")
        }
    }
}

@Composable
fun HtmlEntitiesScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("<div class=\"note\">café & tea</div>") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var encodeAll by rememberSaveable { mutableStateOf(false) }
    var decode by rememberSaveable { mutableStateOf(false) }

    ToolScaffoldWrapper("HTML Entities", "Named entities for HTML text nodes", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !decode, onClick = { decode = false }, label = { Text("Encode") })
                FilterChip(selected = decode, onClick = { decode = true }, label = { Text("Decode") })
                FilterChip(selected = encodeAll, onClick = { encodeAll = !encodeAll }, label = { Text("Encode all chars") })
            }
            CodeEditor(value = input, onValueChange = { input = it }, label = "Input", minLines = 5)
            ActionBar(
                primaryLabel = if (decode) "Decode" else "Encode",
                onPrimary = {
                    scope.launch {
                        val result = compute {
                            if (decode) EncodingTools.htmlDecode(input)
                            else ToolResult.Success(EncodingTools.htmlEncode(input, encodeAll))
                        }
                        when (result) {
                            is ToolResult.Success -> { output = result.value; error = null }
                            is ToolResult.Failure -> { output = ""; error = result.message }
                        }
                    }
                },
            )
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) OutputViewer(output, label = "Result")
        }
    }
}

@Composable
fun UnicodeInspectorScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("a é 😀 中") }
    var output by rememberSaveable { mutableStateOf("") }

    ToolScaffoldWrapper("Unicode Inspector", "Code points, UTF-8 bytes and categories", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CodeEditor(value = input, onValueChange = { input = it }, label = "Character(s)", minLines = 4)
            ActionBar(
                primaryLabel = "Inspect",
                onPrimary = {
                    scope.launch {
                        output = compute {
                            EncodingTools.inspect(input).joinToString("\n\n") { info ->
                                buildString {
                                    appendLine("Character:   ${String(Character.toChars(info.codePoint))}")
                                    appendLine("Code point:  U+%04X".format(info.codePoint))
                                    appendLine("Decimal:     ${info.codePoint}")
                                    appendLine("UTF-8 bytes: ${info.utf8}")
                                    appendLine("UTF-16:      ${info.utf16}")
                                    appendLine("Category:    ${info.category}")
                                    if (info.name.isNotBlank()) append("Name:        ${info.name}")
                                }
                            }
                        }
                    }
                },
            )
            if (output.isNotEmpty()) OutputViewer(output, label = "Inspection", maxLines = 40)
        }
    }
}

@Composable
fun RadixConverterScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("255") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ToolScaffoldWrapper("Number Base Converter", "Hex, decimal, binary and octal", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Prefix with 0x for hex, 0b for binary or 0o for octal. Plain numbers are decimal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CodeEditor(value = input, onValueChange = { input = it }, label = "Number", minLines = 3)
            ActionBar(
                primaryLabel = "Convert",
                onPrimary = {
                    scope.launch {
                        val result = compute { EncodingTools.parseAny(input) }
                        when (result) {
                            is ToolResult.Success -> {
                                val v = result.value
                                output = buildString {
                                    appendLine("Decimal: ${v.decimal}")
                                    appendLine("Hex:     ${v.hex}")
                                    appendLine("Binary:  ${v.binary}")
                                    appendLine("Octal:   ${v.octal}")
                                }
                                error = null
                            }
                            is ToolResult.Failure -> { output = ""; error = result.message }
                        }
                    }
                },
            )
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) OutputViewer(output, label = "Bases")
        }
    }
}

@Composable
fun TimestampScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("1700000000") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var zoneId by rememberSaveable { mutableStateOf(ZoneId.systemDefault().id) }

    ToolScaffoldWrapper("Timestamp Converter", "Unix seconds and milliseconds, with time zone", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Values are auto-detected as seconds or milliseconds; the ambiguity is reported explicitly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CodeEditor(
                value = input,
                onValueChange = { input = it },
                label = "Timestamp (or ISO-8601 date)",
                minLines = 2,
            )
            CodeEditor(
                value = zoneId,
                onValueChange = { zoneId = it },
                label = "Time zone (e.g. UTC, Europe/Paris)",
                minLines = 1,
                maxLines = 1,
            )
            ActionBar(
                primaryLabel = "Convert",
                onPrimary = {
                    scope.launch {
                        val zone = runCatching { ZoneId.of(zoneId.trim()) }.getOrElse {
                            error = "Unknown time zone '${zoneId.trim()}'."
                            return@launch
                        }
                        val result = compute {
                            if (input.trim().toLongOrNull() != null) {
                                DateTimeTools.parseEpoch(input, zone)
                            } else {
                                DateTimeTools.fromDateString(input, zone)
                            }
                        }
                        when (result) {
                            is ToolResult.Success -> {
                                val c = result.value
                                output = buildString {
                                    appendLine("Seconds:      ${c.seconds}")
                                    appendLine("Milliseconds: ${c.epochMillis}")
                                    appendLine("ISO (UTC):    ${c.isoUtc}")
                                    appendLine("Local (${c.zoneId}): ${c.isoLocal}")
                                    appendLine("RFC 1123:     ${c.rfc1123}")
                                    c.note?.let { appendLine("Note:         $it") }
                                }
                                error = null
                            }
                            is ToolResult.Failure -> { output = ""; error = result.message }
                        }
                    }
                },
            )
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) OutputViewer(output, label = "Conversion")
        }
    }
}
