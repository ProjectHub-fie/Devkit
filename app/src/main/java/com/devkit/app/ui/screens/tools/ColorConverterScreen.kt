package com.devkit.app.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.devkit.app.core.ToolResult
import com.devkit.app.core.text.ColorTools
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.OutputViewer
import com.devkit.app.ui.components.StatusBanner
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun ColorConverterScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("#7C6BF2") }
    var output by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var preview by rememberSaveable { mutableStateOf<Int?>(null) }

    ToolScaffoldWrapper("Color Converter", "HEX, RGB and HSL with a live preview", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CodeEditor(
                value = input,
                onValueChange = { input = it },
                label = "Color (#RRGGBB, rgb(...) or hsl)",
                minLines = 2,
                maxLines = 3,
            )
            Button(onClick = {
                when (val result = ColorTools.parse(input)) {
                    is ToolResult.Success -> {
                        val rgb = result.value
                        val hsl = ColorTools.rgbToHsl(rgb)
                        val hex = ColorTools.rgbToHex(rgb)
                        val luminance = ColorTools.relativeLuminance(rgb)
                        val white = ColorTools.Rgb(255, 255, 255)
                        val black = ColorTools.Rgb(0, 0, 0)
                        output = buildString {
                            appendLine("HEX:  $hex")
                            appendLine("RGB:  rgb(${rgb.r}, ${rgb.g}, ${rgb.b})")
                            appendLine("HSL:  hsl(${hsl.h.roundToInt()}, ${hsl.s.roundToInt()}%, ${hsl.l.roundToInt()}%)")
                            appendLine()
                            appendLine("Contrast vs white: %.2f:1".format(ColorTools.contrastRatio(rgb, white)))
                            appendLine("Contrast vs black: %.2f:1".format(ColorTools.contrastRatio(rgb, black)))
                            appendLine("Relative luminance: %.3f".format(luminance))
                        }
                        preview = android.graphics.Color.rgb(rgb.r, rgb.g, rgb.b)
                        error = null
                    }
                    is ToolResult.Failure -> { output = ""; error = result.message; preview = null }
                }
            }) { Text("Convert") }
            error?.let { StatusBanner(it, isError = true) }
            preview?.let { argb ->
                Surface(
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(argb),
                ) {}
            }
            if (output.isNotEmpty()) OutputViewer(output, label = "Values")
        }
    }
}
