package com.devkit.app.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devkit.app.core.ToolResult
import com.devkit.app.ui.components.ActionBar
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.OutputViewer
import com.devkit.app.ui.components.StatusBanner
import com.devkit.app.util.compute
import kotlinx.coroutines.launch

/** Shared tool scaffold so every tool screen has identical chrome. */
@Composable
internal fun ToolScaffoldWrapper(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    com.devkit.app.ui.components.ToolScaffold(
        title = title,
        subtitle = subtitle,
        onBack = onBack,
        actions = actions,
        content = content,
    )
}

/**
 * Generic single-input -> single-output tool screen. Covers the majority of
 * offline tools (formatters, converters, encoders) without duplicating the
 * input/action/output layout thirty times.
 */
@Composable
internal fun TransformToolScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    defaultInput: String = "",
    inputLabel: String = "Input",
    outputLabel: String = "Output",
    runLabel: String = "Convert",
    monospaceInput: Boolean = true,
    minInputLines: Int = 8,
    options: (@Composable () -> Unit)? = null,
    transform: (String) -> ToolResult<String>,
) {
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf(defaultInput) }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by rememberSaveable { mutableStateOf(false) }

    ToolScaffoldWrapper(title, subtitle, onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            options?.invoke()
            CodeEditor(
                value = input,
                onValueChange = { input = it },
                label = inputLabel,
                minLines = minInputLines,
                monospace = monospaceInput,
            )
            ActionBar(
                primaryLabel = runLabel,
                onPrimary = {
                    busy = true
                    scope.launch {
                        val result = compute { transform(input) }
                        busy = false
                        when (result) {
                            is ToolResult.Success -> { output = result.value; error = null }
                            is ToolResult.Failure -> { output = ""; error = result.message }
                        }
                    }
                },
                loading = busy,
            )
            error?.let { StatusBanner(it, isError = true) }
            if (output.isNotEmpty()) {
                OutputViewer(output, label = outputLabel, maxLines = 24)
            }
        }
    }
}
