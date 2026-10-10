package com.devkit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devkit.app.core.ToolCatalog
import com.devkit.app.core.ToolCategory
import com.devkit.app.core.ToolDefinition
import com.devkit.app.data.prefs.UserSettings
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.EmptyState
import com.devkit.app.ui.components.OfflineBanner
import com.devkit.app.ui.components.SearchField
import com.devkit.app.ui.components.SectionHeader
import com.devkit.app.ui.components.ToolCard
import com.devkit.app.util.rememberOnlineState

@Composable
fun HomeScreen(
    repository: DevKitRepository,
    settings: UserSettings,
    onOpenTool: (String) -> Unit,
    onOpenApiTester: () -> Unit,
    onOpenCategory: (ToolCategory) -> Unit,
) {
    val context = LocalContext.current
    val online by rememberOnlineState(context)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val history by repository.toolHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    val favorites by repository.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }

    val favoriteIds = remember(favorites) { favorites.map { it.toolId }.toSet() }
    val toggleFavorite: (String) -> Unit = { toolId ->
        scope.launch { repository.toggleFavorite(toolId, toolId !in favoriteIds) }
    }
    val recentTools = remember(history) {
        history.mapNotNull { ToolCatalog.byId(it.toolId) }
            .distinctBy { it.id }
            .take(6)
    }
    val favoriteTools = remember(favorites, favoriteIds) {
        favorites.sortedByDescending { it.pinned }.mapNotNull { ToolCatalog.byId(it.toolId) }
    }
    val searchResults = remember(query) { ToolCatalog.search(query) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                Text("Dev Kit", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Developer tools, anywhere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { OfflineBanner(offline = !online) }

        item {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search ${ToolCatalog.tools.size} tools",
            )
        }

        if (query.isNotBlank()) {
            item {
                SectionHeader("Results (${searchResults.size})")
            }
            if (searchResults.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.TravelExplore,
                        title = "No matching tools",
                        message = "Try a different word, or browse the categories below.",
                    )
                }
            } else {
                items(searchResults, key = { it.id }) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = { onOpenTool(tool.id) },
                        isFavorite = tool.id in favoriteIds,
                        onFavoriteToggle = {
                            toggleFavorite(tool.id)
                        },
                    )
                }
            }
        } else {
            item {
                SectionHeader("Quick access")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        AssistChip(
                            onClick = onOpenApiTester,
                            label = { Text("API Tester") },
                            leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Bolt, contentDescription = null) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        )
                    }
                    val quick = ToolCatalog.tools.take(6)
                    items(quick, key = { "quick-${it.id}" }) { tool ->
                        AssistChip(
                            onClick = { onOpenTool(tool.id) },
                            label = { Text(tool.title) },
                        )
                    }
                }
            }

            if (recentTools.isNotEmpty()) {
                item { SectionHeader("Recently used") }
                items(recentTools, key = { "recent-${it.id}" }) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = { onOpenTool(tool.id) },
                        isFavorite = tool.id in favoriteIds,
                        onFavoriteToggle = { toggleFavorite(tool.id) },
                    )
                }
            }

            if (favoriteTools.isNotEmpty()) {
                item { SectionHeader("Favorites") }
                items(favoriteTools, key = { "fav-${it.id}" }) { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = { onOpenTool(tool.id) },
                        isFavorite = true,
                        onFavoriteToggle = { toggleFavorite(tool.id) },
                    )
                }
            } else {
                item {
                    EmptyState(
                        icon = Icons.Filled.Star,
                        title = "No favorites yet",
                        message = "Tap the star on any tool to keep it within reach.",
                    )
                }
            }

            item { SectionHeader("Categories") }
            items(ToolCategory.entries.toList(), key = { "cat-${it.id}" }) { category ->
                val count = ToolCatalog.byCategory(category).size
                androidx.compose.material3.OutlinedCard(
                    onClick = { onOpenCategory(category) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(category.label, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "$count tools",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        androidx.compose.material3.Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onOpenApiTester) {
                        Text("Open API Tester")
                    }
                }
            }
        }
    }
}
