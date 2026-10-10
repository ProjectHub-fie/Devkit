package com.devkit.app.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devkit.app.core.ToolResult
import com.devkit.app.core.text.CaseConverter
import com.devkit.app.core.text.CronTools
import com.devkit.app.core.text.DateTimeTools
import com.devkit.app.core.text.DiffTools
import com.devkit.app.core.text.LoremTools
import com.devkit.app.core.text.RegexTools
import com.devkit.app.core.text.TextStats
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.DiffLineRow
import com.devkit.app.ui.components.OutputViewer
import com.devkit.app.ui.components.StatusBanner
import com.devkit.app.ui.theme.LocalSemanticColors
import com.devkit.app.util.compute
import java.time.ZoneId
import kotlinx.coroutines.launch

@Composable
fun WordCountScreen(repository: DevKitRepository, onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    ToolScaffoldWrapper("Word & Character Counter", "Counts update as you type", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CodeEditor(
                value = input,
                onValueChange = { input = it },
                label = "Text",
                minLines = 8,
                monospace = false,
            )
            val counts = TextStats.analyze(input)
            OutputViewer(
                value = buildString {
                    appendLine("Characters:           ${counts.characters}")
                    appendLine("Characters (no space):${counts.charactersNoSpaces}")
                    appendLine("Words:                ${counts.words}")
                    appendLine("Lines:                ${counts.lines}")
                    appendLine("Paragraphs:           ${counts.paragraphs}")
                    append("Sentences:            ${counts.sentences}")
                },
                label = "Statistics",
                maxLines = 10,
            )
            Text(
                "Rules: words are whitespace-separated runs; paragraphs are blank-line separated; characters are UTF-16 code units.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val top = TextStats.wordFrequency(input, 5)
            if (top.isNotEmpty()) {
                OutputViewer(
                    value = top.joinToString("\n") { "${it.first} — ${it.second}" },
                    label = "Top words",
                    maxLines = 8,
                )
            }
        }
    }
}

@Composable
fun CaseConverterScreen(repository: DevKitRepository, onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("dev kit text tool") }
    var output by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    fun apply(name: String, transform: (String) -> String) {
        output = transform(input)
        label = name
    }
    ToolScaffoldWrapper("Case Converter", "Convert text between naming conventions", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CodeEditor(value = input, onValueChange = { input = it }, label = "Input", minLines = 4, monospace = false)
            val options = listOf<Pair<String, (String) -> String>>(
                "UPPER" to CaseConverter::upper,
                "lower" to CaseConverter::lower,
                "Title" to CaseConverter::title,
                "Sentence" to CaseConverter::sentence,
                "camelCase" to CaseConverter::camel,
                "PascalCase" to CaseConverter::pascal,
                "snake_case" to CaseConverter::snake,
                "kebab-case" to CaseConverter::kebab,
                "CONSTANT" to CaseConverter::constant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (name, fn) ->
                            FilterChip(
                                selected = label == name,
                                onClick = { apply(name, fn) },
                                label = { Text(name) },
                            )
                        }
                    }
                }
            }
            if (output.isNotEmpty()) OutputViewer(output, label = label, monospace = false)
        }
    }
}

@Composable
fun TextDiffScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var left by rememberSaveable { mutableStateOf("line one\nline two\nline three") }
    var right by rememberSaveable { mutableStateOf("line one\nline 2\nline three\nline four") }
    var result by remember { mutableStateOf<DiffTools.DiffResult?>(null) }
    val semantics = LocalSemanticColors.current

    ToolScaffoldWrapper("Text Diff", "Line-by-line comparison", onBack) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CodeEditor(
                    value = left,
                    onValueChange = { left = it },
                    label = "Original",
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.weight(1f),
                )
                CodeEditor(
                    value = right,
                    onValueChange = { right = it },
                    label = "Changed",
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.weight(1f),
                )
            }
            Button(onClick = {
                scope.launch {
                    result = compute { DiffTools.diff(left, right) }
                }
            }) { Text("Compare") }
            val diff = result
            if (diff != null) {
                Text(
                    "+${diff.added} added \u00b7 -${diff.removed} removed" + if (diff.truncated) " (truncated)" else "",
                    style = MaterialTheme.typography.labelMedium,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    itemsIndexed(diff.lines) { _, line ->
                        when (line.type) {
                            DiffTools.DiffLine.Type.ADDED -> DiffLineRow("+", line.text, semantics.addedLine)
                            DiffTools.DiffLine.Type.REMOVED -> DiffLineRow("-", line.text, semantics.removedLine)
                            DiffTools.DiffLine.Type.SAME -> DiffLineRow(" ", line.text, androidx.compose.ui.graphics.Color.Transparent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegexTesterScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var pattern by rememberSaveable { mutableStateOf("\\b\\w+@\\w+\\.\\w+\\b") }
    var input by rememberSaveable { mutableStateOf("Contact ada@example.com or linus@kernel.org") }
    var ignoreCase by rememberSaveable { mutableStateOf(false) }
    var multiline by rememberSaveable { mutableStateOf(false) }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var warning by rememberSaveable { mutableStateOf<String?>(null) }

    ToolScaffoldWrapper("Regex Tester", "Safe pattern matching with a complexity guard", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CodeEditor(value = pattern, onValueChange = { pattern = it }, label = "Pattern", minLines = 2, maxLines = 4)
            CodeEditor(value = input, onValueChange = { input = it }, label = "Test text", minLines = 6)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = ignoreCase, onClick = { ignoreCase = !ignoreCase }, label = { Text("Ignore case") })
                FilterChip(selected = multiline, onClick = { multiline = !multiline }, label = { Text("Multiline") })
            }
            Button(onClick = {
                scope.launch {
                    val r = compute { RegexTools.test(pattern, input, ignoreCase, multiline) }
                    warning = r.safetyWarning
                    if (r.compileError != null) {
                        error = r.compileError; output = ""
                    } else {
                        error = null
                        output = buildString {
                            appendLine("Matches: ${r.matches.size}")
                            if (r.truncated) appendLine("(input was truncated for safety)")
                            appendLine()
                            r.matches.take(200).forEach { m ->
                                appendLine("[${m.start}..${m.end}] ${m.value}")
                                m.groups.forEachIndexed { i, g ->
                                    if (g != null) appendLine("    group ${i + 1}: $g")
                                }
                            }
                        }
                    }
                }
            }) { Text("Test pattern") }
            warning?.let { StatusBanner(it, isError = false) }
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) OutputViewer(output, label = "Results", maxLines = 30)
        }
    }
}

@Composable
fun LoremIpsumScreen(repository: DevKitRepository, onBack: () -> Unit) {
    var unit by rememberSaveable { mutableStateOf(LoremTools.Unit.PARAGRAPHS) }
    var amount by rememberSaveable { mutableStateOf(3) }
    var output by rememberSaveable { mutableStateOf("") }

    ToolScaffoldWrapper("Lorem Ipsum", "Placeholder text generator", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LoremTools.Unit.entries.forEach { candidate ->
                    FilterChip(
                        selected = unit == candidate,
                        onClick = { unit = candidate },
                        label = { Text(candidate.name.lowercase()) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5, 10).forEach { value ->
                    FilterChip(
                        selected = amount == value,
                        onClick = { amount = value },
                        label = { Text("$value") },
                    )
                }
            }
            Button(onClick = { output = LoremTools.generate(unit, amount) }) { Text("Generate") }
            if (output.isNotEmpty()) OutputViewer(output, label = "Lorem ipsum", monospace = false, maxLines = 40)
        }
    }
}

@Composable
fun CronHelperScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var expression by rememberSaveable { mutableStateOf("*/15 9-17 * * 1-5") }
    var output by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }

    ToolScaffoldWrapper("Cron Expression Helper", "Validate and explain cron fields", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CodeEditor(value = expression, onValueChange = { expression = it }, label = "Expression", minLines = 2, maxLines = 3)
            Text(
                "Supports 5-field cron (minute hour day month weekday) and optional 6/7-field variants. Validation only — Dev Kit never schedules or runs jobs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = {
                scope.launch {
                    val analysis = compute { CronTools.analyze(expression) }
                    isError = !analysis.valid
                    output = buildString {
                        appendLine(analysis.message)
                        if (analysis.fields.isNotEmpty()) {
                            appendLine()
                            analysis.fields.forEach { field ->
                                appendLine("${field.name.padEnd(13)} ${field.value.padEnd(12)} ${field.explanation}")
                            }
                        }
                    }
                }
            }) { Text("Validate") }
            if (output.isNotEmpty()) StatusBanner(output, isError = isError)
        }
    }
}

@Composable
fun DateConverterScreen(repository: DevKitRepository, onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("2024-02-29T12:30:00Z") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ToolScaffoldWrapper("Date & Time Converter", "Convert dates to timestamps and back", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CodeEditor(
                value = input,
                onValueChange = { input = it },
                label = "ISO-8601 date or \"yyyy-MM-dd HH:mm:ss\"",
                minLines = 2,
            )
            Button(onClick = {
                val zone = ZoneId.systemDefault()
                val result = if (input.trim().toLongOrNull() != null) {
                    DateTimeTools.parseEpoch(input, zone)
                } else {
                    DateTimeTools.fromDateString(input, zone)
                }
                when (result) {
                    is ToolResult.Success -> {
                        val c = result.value
                        output = buildString {
                            appendLine("Seconds:      ${c.seconds}")
                            appendLine("Milliseconds: ${c.epochMillis}")
                            appendLine("ISO (UTC):    ${c.isoUtc}")
                            appendLine("Local:        ${c.isoLocal} (${c.zoneId})")
                            appendLine("RFC 1123:     ${c.rfc1123}")
                        }
                        error = null
                    }
                    is ToolResult.Failure -> { output = ""; error = result.message }
                }
            }) { Text("Convert") }
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) OutputViewer(output, label = "Result")
        }
    }
}
