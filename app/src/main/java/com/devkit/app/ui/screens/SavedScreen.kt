package com.devkit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devkit.app.core.ToolCatalog
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.EmptyState
import com.devkit.app.ui.components.SimpleList
import com.devkit.app.ui.components.ToolCard
import com.devkit.app.ui.theme.CodeTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    repository: DevKitRepository,
    onOpenTool: (String) -> Unit,
    onOpenApiTester: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    val favorites by repository.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val snippets by repository.snippets.collectAsStateWithLifecycle(initialValue = emptyList())
    val requests by repository.savedRequests.collectAsStateWithLifecycle(initialValue = emptyList())
    val tabs = listOf("Tools", "Snippets", "Requests")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved", style = MaterialTheme.typography.titleLarge) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
            when (tab) {
                0 -> {
                    val favoriteTools = favorites.mapNotNull { ToolCatalog.byId(it.toolId) }
                    if (favoriteTools.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.Bookmarks,
                            title = "No favorite tools",
                            message = "Star a tool from Home or Tools to pin it here.",
                        )
                    } else {
                        SimpleList(
                            items = favoriteTools,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            key = { it.id },
                        ) { tool ->
                            ToolCard(tool = tool, onClick = { onOpenTool(tool.id) }, isFavorite = true)
                        }
                    }
                }
                1 -> {
                    if (snippets.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.Bookmarks,
                            title = "No snippets saved",
                            message = "Open the Snippets tool to save reusable code.",
                            action = {
                                androidx.compose.material3.TextButton(onClick = { onOpenTool("snippets") }) {
                                    Text("Open Snippets")
                                }
                            },
                        )
                    } else {
                        SimpleList(
                            items = snippets,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            key = { it.id },
                        ) { snippet ->
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(snippet.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        snippet.code,
                                        style = CodeTextStyle,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (requests.isEmpty()) {
                        EmptyState(
                            icon = Icons.Filled.Cloud,
                            title = "No saved requests",
                            message = "Save API requests to reuse them later.",
                            action = {
                                androidx.compose.material3.TextButton(onClick = onOpenApiTester) {
                                    Text("Open API Tester")
                                }
                            },
                        )
                    } else {
                        SimpleList(
                            items = requests,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            key = { it.id },
                        ) { request ->
                            ElevatedCard(onClick = onOpenApiTester, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(request.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${request.method} ${request.displayUrl}",
                                        style = CodeTextStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
