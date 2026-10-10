package com.devkit.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.devkit.app.core.ToolCatalog
import com.devkit.app.core.ToolCategory
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.CategoryHeader
import com.devkit.app.ui.components.EmptyState
import com.devkit.app.ui.components.SearchField
import com.devkit.app.ui.components.ToolCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    repository: DevKitRepository,
    onOpenTool: (String) -> Unit,
) {
    val favorites by repository.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ToolCategory?>(null) }

    val favoriteIds = remember(favorites) { favorites.map { it.toolId }.toSet() }
    val toggleFavorite: (String) -> Unit = { id ->
        scope.launch { repository.toggleFavorite(id, id !in favoriteIds) }
    }

    val results = remember(query, selectedCategory) {
        val base = if (query.isBlank()) ToolCatalog.tools else ToolCatalog.search(query)
        selectedCategory?.let { cat -> base.filter { it.category == cat } } ?: base
    }
    val grouped = remember(results) { results.groupBy { it.category } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tools", style = MaterialTheme.typography.titleLarge) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search tools",
                )
            }
            item {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        AssistChip(
                            onClick = { selectedCategory = null },
                            label = { Text("All") },
                            enabled = true,
                        )
                    }
                    items(ToolCategory.entries.toList(), key = { it.id }) { category ->
                        AssistChip(
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            enabled = selectedCategory != category || true,
                            colors = if (selectedCategory == category) {
                                androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                )
                            } else androidx.compose.material3.AssistChipDefaults.assistChipColors(),
                        )
                    }
                }
            }

            if (results.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.TravelExplore,
                        title = "No tools match",
                        message = "Clear the filters or try another search term.",
                    )
                }
            } else {
                ToolCategory.entries.forEach { category ->
                    val tools = grouped[category] ?: return@forEach
                    item(key = "header-${category.id}") {
                        CategoryHeader(category, tools.size)
                    }
                    items(tools, key = { it.id }) { tool ->
                        ToolCard(
                            tool = tool,
                            onClick = { onOpenTool(tool.id) },
                            isFavorite = tool.id in favoriteIds,
                            onFavoriteToggle = { toggleFavorite(tool.id) },
                        )
                    }
                }
            }
        }
    }
}
