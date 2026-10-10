package com.devkit.app.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devkit.app.core.ToolResult
import com.devkit.app.core.json.JsonTools
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.ActionBar
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.OutputViewer
import com.devkit.app.ui.components.StatusBanner
import com.devkit.app.util.compute
import kotlinx.coroutines.launch

@Composable
fun JsonFormatterScreen(repository: DevKitRepository, onBack: () -> Unit, onOpenApiTester: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf(DEFAULT_JSON) }
    var output by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var indent by remember { mutableStateOf(2) }
    var busy by remember { mutableStateOf(false) }

    fun run(transform: (String) -> ToolResult<String>) {
        busy = true
        scope.launch {
            val result = compute { transform(input) }
            busy = false
            when (result) {
                is ToolResult.Success -> { output = result.value; error = null }
                is ToolResult.Failure -> { output = ""; error = result.message }
            }
        }
    }

    ToolScaffoldWrapper("JSON Formatter", "Pretty-print, minify and validate", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Indent", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium)
                listOf(2, 4, 8).forEach { value ->
                    FilterChip(
                        selected = indent == value,
                        onClick = { indent = value },
                        label = { Text("$value") },
                    )
                }
            }
            CodeEditor(
                value = input,
                onValueChange = { input = it },
                label = "Input JSON",
                placeholder = "{ \"hello\": \"world\" }",
                minLines = 8,
            )
            ActionBar(
                primaryLabel = "Format",
                onPrimary = { run { JsonTools.prettyPrint(it, indent) } },
                loading = busy,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { run { JsonTools.minify(it) } }, label = { Text("Minify") })
                AssistChip(onClick = { run { JsonTools.prettyPrint(it, indent) } }, label = { Text("Pretty") })
                AssistChip(
                    onClick = {
                        run { text ->
                            when (val r = JsonTools.validate(text)) {
                                is ToolResult.Success -> ToolResult.Success("Valid JSON ✓")
                                is ToolResult.Failure -> r
                            }
                        }
                    },
                    label = { Text("Validate") },
                )
            }
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) {
                OutputViewer(output, label = "Formatted output", maxLines = 24)
            }
        }
    }
}

private const val DEFAULT_JSON =
    """{"name":"Dev Kit","version":1,"offline":true,"tools":["json","base64","uuid"]}"""
