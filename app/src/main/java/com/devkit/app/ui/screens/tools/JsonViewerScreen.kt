package com.devkit.app.ui.screens.tools

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devkit.app.core.ToolResult
import com.devkit.app.core.json.JsonNode
import com.devkit.app.core.json.JsonTools
import com.devkit.app.core.json.asLiteral
import com.devkit.app.core.json.summary
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.ActionBar
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.EmptyState
import com.devkit.app.ui.components.StatusBanner
import com.devkit.app.ui.theme.CodeTextStyle

private data class FlatRow(val depth: Int, val label: String?, val node: JsonNode, val path: String)

@Composable
fun JsonViewerScreen(repository: DevKitRepository, onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("""{"user":{"id":1,"name":"Ada","roles":["admin","dev"]},"active":true}""") }
    var tree by remember { mutableStateOf<JsonNode?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }

    ToolScaffoldWrapper("JSON Viewer", "Expand and collapse nested nodes", onBack) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CodeEditor(
                value = input,
                onValueChange = { input = it },
                label = "JSON",
                minLines = 4,
                maxLines = 6,
            )
            ActionBar(
                primaryLabel = "View tree",
                onPrimary = {
                    when (val result = JsonTools.parseTree(input)) {
                        is ToolResult.Success -> { tree = result.value; error = null }
                        is ToolResult.Failure -> { tree = null; error = result.message }
                    }
                },
            )
            error?.let { StatusBanner(it, isError = true) }
            val node = tree
            if (node == null) {
                EmptyState(
                    icon = Icons.Filled.KeyboardArrowRight,
                    title = "Nothing loaded",
                    message = "Paste JSON above and choose View tree.",
                )
            } else {
                val rows = remember(node, collapsed.toMap()) { flatten(node, depth = 0, label = null, path = "$", collapsed = collapsed) }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    items(rows.size, key = { rows[it].path }) { index ->
                        val row = rows[index]
                        JsonRow(
                            row = row,
                            collapsed = collapsed[row.path] == true,
                            onToggle = { collapsed[row.path] = collapsed[row.path] != true },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JsonRow(row: FlatRow, collapsed: Boolean, onToggle: () -> Unit) {
    val expandable = row.node is JsonNode.ObjectNode || row.node is JsonNode.ArrayNode
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = expandable, onClick = onToggle)
            .padding(start = (row.depth * 16).dp, top = 3.dp, bottom = 3.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expandable) {
            Icon(
                imageVector = if (collapsed) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (collapsed) "Expand" else "Collapse",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(18.dp),
            )
        } else {
            Spacer(Modifier.width(18.dp))
        }
        if (row.label != null) {
            Text(
                "${row.label}: ",
                style = CodeTextStyle.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = row.node.summary(),
            style = CodeTextStyle,
            color = if (row.node is JsonNode.ValueNode) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun flatten(
    node: JsonNode,
    depth: Int,
    label: String?,
    path: String,
    collapsed: Map<String, Boolean>,
): List<FlatRow> {
    val rows = mutableListOf(FlatRow(depth, label, node, path))
    val isCollapsed = collapsed[path] == true
    if (!isCollapsed) {
        when (node) {
            is JsonNode.ObjectNode -> node.entries.forEach { (key, child) ->
                rows.addAll(flatten(child, depth + 1, key, "$path.$key", collapsed))
            }
            is JsonNode.ArrayNode -> node.items.forEachIndexed { index, child ->
                rows.addAll(flatten(child, depth + 1, "[$index]", "$path[$index]", collapsed))
            }
            else -> Unit
        }
    }
    return rows
}
