package com.devkit.app.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.EmptyState
import com.devkit.app.ui.components.SearchField
import com.devkit.app.ui.components.SimpleList
import com.devkit.app.ui.theme.CodeTextStyle
import kotlinx.coroutines.launch

@Composable
fun SnippetsScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("kotlin") }
    var code by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Long?>(null) }

    val snippets by repository.snippets.collectAsStateWithLifecycle(initialValue = emptyList())
    val visible = remember(snippets, query) {
        if (query.isBlank()) snippets
        else snippets.filter {
            it.title.contains(query, true) || it.language.contains(query, true) || it.code.contains(query, true)
        }
    }

    ToolScaffoldWrapper(
        title = "Snippets",
        subtitle = "Saved locally and searchable",
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                title = ""; language = "kotlin"; code = ""; editing = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "New snippet")
            }
        },
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchField(value = query, onValueChange = { query = it }, placeholder = "Search snippets")

            if (editing) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Language") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CodeEditor(value = code, onValueChange = { code = it }, label = "Code", minLines = 5)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = title.isNotBlank() && code.isNotBlank(),
                        onClick = {
                            scope.launch {
                                repository.saveSnippet(title.trim(), language.trim(), code)
                                editing = false
                            }
                        },
                    ) { Text("Save") }
                    TextButton(onClick = { editing = false }) { Text("Cancel") }
                }
            }

            if (visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Code,
                    title = if (query.isBlank()) "No snippets yet" else "No matches",
                    message = if (query.isBlank())
                        "Save reusable code with the + button. Everything stays on this device."
                    else "Try another search term.",
                )
            } else {
                SimpleList(items = visible, key = { it.id }) { snippet ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row {
                                Column(Modifier.weight(1f)) {
                                    Text(snippet.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        snippet.language,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { deleteTarget = snippet.id }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete snippet")
                                }
                            }
                            Text(
                                snippet.code,
                                style = CodeTextStyle,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                com.devkit.app.ui.components.CopyIconButton(snippet.code, "Copy snippet")
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete snippet?") },
            text = { Text("This removes the snippet from this device. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteSnippet(id) }
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
}
