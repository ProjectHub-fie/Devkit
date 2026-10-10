package com.devkit.app.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.devkit.app.core.ToolResult
import com.devkit.app.core.json.CsvTools
import com.devkit.app.core.json.XmlTools
import com.devkit.app.core.json.YamlTools
import com.devkit.app.core.text.MarkdownTools
import com.devkit.app.core.text.SqlFormatter
import com.devkit.app.core.text.StringEscape
import com.devkit.app.core.web.JwtTools
import com.devkit.app.core.web.HashInspector
import com.devkit.app.core.web.UrlParser
import com.devkit.app.data.repo.DevKitRepository

@Composable
fun XmlFormatterScreen(repository: DevKitRepository, onBack: () -> Unit) = TransformToolScreen(
    title = "XML Formatter",
    subtitle = "Format and validate XML (external entities disabled)",
    onBack = onBack,
    defaultInput = "<root><item id=\"1\">Dev Kit</item></root>",
    inputLabel = "XML input",
    outputLabel = "Formatted XML",
    runLabel = "Format",
    transform = { XmlTools.format(it) },
)

@Composable
fun SqlFormatterScreen(repository: DevKitRepository, onBack: () -> Unit) = TransformToolScreen(
    title = "SQL Formatter",
    subtitle = "Format common SQL statements (keyword based)",
    onBack = onBack,
    defaultInput = "select id, name from users where active = true and role = 'admin' order by name;",
    inputLabel = "SQL input",
    outputLabel = "Formatted SQL",
    runLabel = "Format",
    transform = { ToolResult.Success(SqlFormatter.format(it)) },
)

@Composable
fun JsonYamlScreen(repository: DevKitRepository, onBack: () -> Unit) {
    var toYaml by remember { mutableStateOf(true) }
    TransformToolScreen(
        title = "JSON \u2194 YAML",
        subtitle = "Convert between JSON and YAML with a safe parser",
        onBack = onBack,
        defaultInput = "{\n  \"service\": \"devkit\",\n  \"enabled\": true,\n  \"ports\": [8080, 8443]\n}",
        inputLabel = "Input",
        outputLabel = "Output",
        runLabel = if (toYaml) "JSON \u2192 YAML" else "YAML \u2192 JSON",
        options = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = toYaml, onClick = { toYaml = true }, label = { Text("JSON \u2192 YAML") })
                FilterChip(selected = !toYaml, onClick = { toYaml = false }, label = { Text("YAML \u2192 JSON") })
            }
        },
        transform = { if (toYaml) YamlTools.jsonToYaml(it) else YamlTools.yamlToJson(it) },
    )
}

@Composable
fun MarkdownPreviewScreen(repository: DevKitRepository, onBack: () -> Unit) = TransformToolScreen(
    title = "Markdown Preview",
    subtitle = "HTML-escaped render — scripts are never executed",
    onBack = onBack,
    defaultInput = "# Dev Kit\n\n- Offline first\n- **Fast** tools\n\n`inline code` and a [link](https://example.com)",
    inputLabel = "Markdown",
    outputLabel = "Safe HTML output",
    runLabel = "Render",
    transform = { ToolResult.Success(MarkdownTools.toHtml(it)) },
)

@Composable
fun JwtDecoderScreen(repository: DevKitRepository, onBack: () -> Unit) = TransformToolScreen(
    title = "JWT Decoder",
    subtitle = "Decodes header and payload — the signature is NOT verified",
    onBack = onBack,
    inputLabel = "JWT",
    outputLabel = "Decoded token",
    runLabel = "Decode",
    transform = { token ->
        when (val r = JwtTools.decode(token)) {
            is ToolResult.Failure -> r
            is ToolResult.Success -> {
                val v = r.value
                val summary = buildString {
                    appendLine("Algorithm: ${v.algorithm ?: "unknown"}")
                    when (v.expired) {
                        true -> appendLine("Status: EXPIRED at ${v.expiresAt}")
                        false -> appendLine("Status: not expired (exp ${v.expiresAt})")
                        null -> appendLine("Status: no exp claim")
                    }
                    appendLine("Signature present: ${v.signaturePresent}")
                    appendLine()
                    appendLine("Header:")
                    appendLine(v.header)
                    appendLine()
                    appendLine("Payload:")
                    appendLine(v.payload)
                    append("Note: signature verification is not performed by Dev Kit.")
                }
                ToolResult.Success(summary)
            }
        }
    },
)

@Composable
fun HashInspectorScreen(repository: DevKitRepository, onBack: () -> Unit) = TransformToolScreen(
    title = "Hash Inspector",
    subtitle = "Identify a digest by length and compare two hashes",
    onBack = onBack,
    inputLabel = "Hex digest",
    outputLabel = "Likely algorithms",
    runLabel = "Identify",
    transform = { hash ->
        when (val r = HashInspector.identify(hash)) {
            is ToolResult.Failure -> r
            is ToolResult.Success -> ToolResult.Success(
                r.value.joinToString("\n") { "${it.algorithm} (${it.hexLength} hex chars)" }
            )
        }
    },
)

@Composable
fun UrlParserScreen(repository: DevKitRepository, onBack: () -> Unit) = TransformToolScreen(
    title = "URL Parser",
    subtitle = "Break a URL into scheme, host, path and query",
    onBack = onBack,
    defaultInput = "https://api.example.com:8443/v1/users?page=2&limit=50#results",
    inputLabel = "URL",
    outputLabel = "Parsed components",
    runLabel = "Parse",
    transform = { raw ->
        when (val r = UrlParser.parse(raw)) {
            is ToolResult.Failure -> r
            is ToolResult.Success -> {
                val c = r.value
                ToolResult.Success(
                    buildString {
                        appendLine("Scheme:   ${c.scheme}")
                        appendLine("Host:     ${c.host}")
                        appendLine("Port:     ${c.port}")
                        appendLine("User:     ${c.userInfo ?: "-"}")
                        appendLine("Path:     ${c.path}")
                        appendLine("Fragment: ${c.fragment ?: "-"}")
                        appendLine("Query:")
                        if (c.queryParams.isEmpty()) appendLine("  (none)")
                        else c.queryParams.forEach { appendLine("  ${it.first} = ${it.second}") }
                    }
                )
            }
        }
    },
)

@Composable
fun CsvJsonScreen(repository: DevKitRepository, onBack: () -> Unit) = TransformToolScreen(
    title = "CSV to JSON",
    subtitle = "First row is used as the key set",
    onBack = onBack,
    defaultInput = "id,name,role\n1,Ada,admin\n2,Linus,user",
    inputLabel = "CSV",
    outputLabel = "JSON array",
    runLabel = "Convert",
    transform = { CsvTools.toJson(it) },
)

@Composable
fun StringEscapeScreen(repository: DevKitRepository, onBack: () -> Unit) {
    var target = remember { mutableStateOf(StringEscape.Target.JSON) }
    TransformToolScreen(
        title = "String Escaper",
        subtitle = "Escape text for a target language",
        onBack = onBack,
        defaultInput = "He said \"hello\"\n\tand left.",
        inputLabel = "Input",
        outputLabel = "Escaped",
        runLabel = "Escape",
        options = {
            TargetChips(target.value) { target.value = it }
        },
        transform = { ToolResult.Success(StringEscape.escape(it, target.value)) },
    )
}

@Composable
private fun TargetChips(
    selected: StringEscape.Target,
    onSelect: (StringEscape.Target) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StringEscape.Target.entries.forEach { candidate ->
            FilterChip(
                selected = candidate == selected,
                onClick = { onSelect(candidate) },
                label = { Text(candidate.name) },
            )
        }
    }
}
