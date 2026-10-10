package com.devkit.app.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devkit.app.core.json.JsonTools
import com.devkit.app.core.web.ApiOutcome
import com.devkit.app.core.web.ApiRequest
import com.devkit.app.core.web.ApiResponse
import com.devkit.app.core.web.BodyType
import com.devkit.app.core.web.HttpMethod
import com.devkit.app.core.web.SensitiveData
import com.devkit.app.data.local.CollectionEntity
import com.devkit.app.data.local.EnvironmentEntity
import com.devkit.app.data.local.RequestHistoryEntity
import com.devkit.app.data.local.SavedRequestEntity
import com.devkit.app.data.prefs.UserSettings
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.CopyIconButton
import com.devkit.app.ui.components.EmptyState
import com.devkit.app.ui.components.OutputViewer
import com.devkit.app.ui.components.SectionHeader
import com.devkit.app.ui.components.StatusBanner
import com.devkit.app.ui.screens.api.ApiTesterViewModel
import com.devkit.app.ui.screens.api.EditableHeader
import com.devkit.app.ui.screens.api.EditableParam
import com.devkit.app.ui.theme.CodeTextStyle
import com.devkit.app.ui.theme.CodeTextStyleSmall
import com.devkit.app.ui.theme.LocalSemanticColors
import com.devkit.app.util.shareTextFile
import kotlinx.coroutines.launch

private enum class ApiTab(val label: String) { PARAMS("Params"), HEADERS("Headers"), BODY("Body"), SAVED("Saved"), HISTORY("History") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTesterScreen(
    repository: DevKitRepository,
    settings: UserSettings,
) {
    val viewModel: ApiTesterViewModel = viewModel(factory = ApiTesterViewModelFactory(repository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val environments by viewModel.environments.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(ApiTab.PARAMS) }
    var pendingRequest by remember { mutableStateOf<ApiRequest?>(null) }
    var pendingWarning by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showEnvironmentDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedEnvironment by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) { viewModel.setTimeout(settings.defaultTimeoutSeconds) }
    LaunchedEffect(settings.followRedirects) { viewModel.setFollowRedirects(settings.followRedirects) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val payload = repository.exportCollections(includeSecrets = false)
            context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@launch
            when (val result = repository.importCollections(text)) {
                is DevKitRepository.ImportResult.Success -> viewModel.showNotice("Imported ${result.count} request(s).")
                is DevKitRepository.ImportResult.Failure -> viewModel.showNotice(result.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("API Tester", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Requests run directly from this device",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ---- Request line ----
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MethodDropdown(state.method, viewModel::setMethod)
                OutlinedTextField(
                    value = state.url,
                    onValueChange = viewModel::setUrl,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("https://api.example.com/v1/users", style = CodeTextStyleSmall) },
                    textStyle = CodeTextStyleSmall,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        viewModel.prepare(selectedEnvironment) { request, warning ->
                            if (warning != null && settings.warnOnCrossOriginSecrets) {
                                pendingRequest = request
                                pendingWarning = warning
                            } else {
                                viewModel.send(request)
                            }
                        }
                    },
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send")
                }
                OutlinedButton(onClick = viewModel::cancel, enabled = state.loading) {
                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel")
                }
            }

            // ---- Environment + options row ----
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                EnvironmentSelector(
                    environments = environments,
                    selectedId = selectedEnvironment,
                    onSelect = { selectedEnvironment = it },
                    onManage = { showEnvironmentDialog = true },
                )
                Spacer(Modifier.weight(1f))
                TimeoutSelector(state.timeoutSeconds, viewModel::setTimeout)
            }

            TabRow(selectedTabIndex = tab.ordinal) {
                ApiTab.entries.forEach { entry ->
                    Tab(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        text = { Text(entry.label, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }

            Box(Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    when (tab) {
                        ApiTab.PARAMS -> ParamEditor(state.params, viewModel::addParam, viewModel::updateParam, viewModel::removeParam)
                        ApiTab.HEADERS -> HeaderEditor(state.headers, viewModel::addHeader, viewModel::updateHeader, viewModel::removeHeader)
                        ApiTab.BODY -> BodyEditor(state, viewModel)
                        ApiTab.SAVED -> SavedRequestsTab(
                            repository = repository,
                            collections = collections,
                            onLoad = { viewModel.loadRequest(it); tab = ApiTab.PARAMS },
                            onExport = { exportLauncher.launch("devkit-collections.json") },
                            onImport = { importLauncher.launch(arrayOf("application/json")) },
                            onSaveCurrent = { showSaveDialog = true },
                        )
                        ApiTab.HISTORY -> HistoryTab(history) { showClearConfirm = true }
                    }

                    Spacer(Modifier.height(12.dp))
                    ResponsePanel(
                        loading = state.loading,
                        response = state.response,
                        error = state.error,
                        notice = state.notice,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    pendingWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = { pendingWarning = null },
            title = { Text("Send sensitive headers?") },
            text = { Text(warning) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRequest?.let(viewModel::send)
                    pendingWarning = null
                    pendingRequest = null
                }) { Text("Send anyway") }
            },
            dismissButton = { TextButton(onClick = { pendingWarning = null; pendingRequest = null }) { Text("Cancel") } },
        )
    }

    if (showSaveDialog) {
        SaveRequestDialog(
            collections = collections,
            onDismiss = { showSaveDialog = false },
            onSave = { name, collectionId ->
                viewModel.prepare(selectedEnvironment) { request, _ ->
                    scope.launch {
                        repository.saveRequest(
                            id = state.currentRequestId,
                            collectionId = collectionId,
                            name = name,
                            method = request.method.name,
                            url = request.url,
                            headers = request.headers.filter { it.enabled }.map { it.name to it.value },
                            queryParams = request.queryParams.filter { it.enabled }.map { it.name to it.value },
                            bodyType = request.bodyType.name,
                            body = request.body,
                            timeoutSeconds = request.timeoutSeconds,
                        )
                        viewModel.showNotice("Saved \"$name\".")
                    }
                }
                showSaveDialog = false
            },
        )
    }

    if (showEnvironmentDialog) {
        EnvironmentDialog(
            environments = environments,
            repository = repository,
            onDismiss = { showEnvironmentDialog = false },
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear request history?") },
            text = { Text("History stores only redacted URLs and status codes. Clearing cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.clearRequestHistory() }
                    showClearConfirm = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } },
        )
    }
}

private class ApiTesterViewModelFactory(
    private val repository: DevKitRepository,
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ApiTesterViewModel(repository) as T
    }
}

@Composable
private fun MethodDropdown(
    method: HttpMethod,
    onSelect: (HttpMethod) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(method.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HttpMethod.entries.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text(candidate.name) },
                    onClick = { onSelect(candidate); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun TimeoutSelector(seconds: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(onClick = { expanded = true }, label = { Text("Timeout ${seconds}s") })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(5, 15, 30, 60, 120).forEach { value ->
                DropdownMenuItem(text = { Text("${value}s") }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}

@Composable
private fun EnvironmentSelector(
    environments: List<EnvironmentEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onManage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        val label = environments.firstOrNull { it.id == selectedId }?.name ?: "No environment"
        AssistChip(onClick = { expanded = true }, label = { Text(label) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("No environment") }, onClick = { onSelect(null); expanded = false })
            environments.forEach { env ->
                DropdownMenuItem(text = { Text(env.name) }, onClick = { onSelect(env.id); expanded = false })
            }
            HorizontalDivider()
            DropdownMenuItem(text = { Text("Manage environments…") }, onClick = { onManage(); expanded = false })
        }
    }
}

@Composable
private fun HeaderEditor(
    headers: List<EditableHeader>,
    onAdd: () -> Unit,
    onUpdate: (Int, EditableHeader) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Sensitive headers (Authorization, Cookie, API keys) are redacted in history and exports by default.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        headers.forEachIndexed { index, header ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = header.name,
                    onValueChange = { onUpdate(index, header.copy(name = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Name") },
                    singleLine = true,
                    textStyle = CodeTextStyleSmall,
                )
                OutlinedTextField(
                    value = header.value,
                    onValueChange = { onUpdate(index, header.copy(value = it)) },
                    modifier = Modifier.weight(1.4f),
                    label = { Text(if (SensitiveData.isSensitiveHeader(header.name)) "Value (secret)" else "Value") },
                    singleLine = true,
                    textStyle = CodeTextStyleSmall,
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove header")
                }
            }
        }
        OutlinedButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add header")
        }
    }
}

@Composable
private fun ParamEditor(
    params: List<EditableParam>,
    onAdd: () -> Unit,
    onUpdate: (Int, EditableParam) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        params.forEachIndexed { index, param ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = param.name,
                    onValueChange = { onUpdate(index, param.copy(name = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Key") },
                    singleLine = true,
                    textStyle = CodeTextStyleSmall,
                )
                OutlinedTextField(
                    value = param.value,
                    onValueChange = { onUpdate(index, param.copy(value = it)) },
                    modifier = Modifier.weight(1.4f),
                    label = { Text("Value") },
                    singleLine = true,
                    textStyle = CodeTextStyleSmall,
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove parameter")
                }
            }
        }
        OutlinedButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add parameter")
        }
    }
}

@Composable
private fun BodyEditor(state: com.devkit.app.ui.screens.api.ApiUiState, viewModel: ApiTesterViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BodyType.entries.forEach { type ->
                FilterChip(
                    selected = state.bodyType == type,
                    onClick = { viewModel.setBodyType(type) },
                    label = { Text(type.name) },
                )
            }
        }
        CodeEditor(
            value = state.body,
            onValueChange = viewModel::setBody,
            label = "Body",
            placeholder = if (state.bodyType == BodyType.JSON) "{\n  \"key\": \"value\"\n}" else "",
            minLines = 8,
            maxLines = 14,
        )
        if (state.bodyType == BodyType.JSON) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val formatted = JsonTools.prettyPrint(state.body, 2)
                    if (formatted is com.devkit.app.core.ToolResult.Success) viewModel.setBody(formatted.value)
                    else viewModel.showNotice("Body is not valid JSON.")
                }) { Text("Format JSON") }
                OutlinedButton(onClick = {
                    val validation = JsonTools.validate(state.body)
                    viewModel.showNotice(
                        if (validation is com.devkit.app.core.ToolResult.Success) "Body is valid JSON."
                        else (validation as com.devkit.app.core.ToolResult.Failure).message
                    )
                }) { Text("Validate") }
            }
        }
    }
}

@Composable
private fun SavedRequestsTab(
    repository: DevKitRepository,
    collections: List<CollectionEntity>,
    onLoad: (SavedRequestEntity) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onSaveCurrent: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var requests by remember { mutableStateOf<List<SavedRequestEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(collections, query) {
        val flow = if (query.isBlank()) repository.savedRequests else repository.searchSavedRequests(query)
        flow.collect { requests = it }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSaveCurrent, modifier = Modifier.weight(1f)) { Text("Save current request") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Export") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Import") }
        }
        Text(
            "Exports redact sensitive header values and secret-looking bodies. Import validates the schema before saving.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search saved requests") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (requests.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Add,
                title = "No saved requests",
                message = "Save a request, or import a Dev Kit collection file.",
            )
        } else {
            requests.forEach { entity ->
                SavedRequestRow(
                    entity = entity,
                    collectionName = collections.firstOrNull { it.id == entity.collectionId }?.name,
                    onLoad = { onLoad(entity) },
                    onDelete = { scope.launch { repository.deleteSavedRequest(entity.id) } },
                )
            }
        }
    }
}

@Composable
private fun SavedRequestRow(
    entity: SavedRequestEntity,
    collectionName: String?,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onLoad)) {
                Text(entity.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${entity.method} ${entity.displayUrl}",
                    style = CodeTextStyleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                collectionName?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete request")
            }
        }
    }
}

@Composable
private fun HistoryTab(history: List<RequestHistoryEntity>, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent requests", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Clear") }
        }
        if (history.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.PlayArrow,
                title = "No requests yet",
                message = "Send a request and it will appear here with a redacted URL.",
            )
        } else {
            history.forEach { entry ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${entry.method} ${entry.displayUrl}",
                                style = CodeTextStyleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${entry.outcomeLabel} · ${entry.durationMillis} ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponsePanel(
    loading: Boolean,
    response: ApiResponse?,
    error: ApiOutcome.NetworkError?,
    notice: String?,
) {
    val semantics = LocalSemanticColors.current
    var prettyJson by remember { mutableStateOf(true) }
    var showHeaders by remember { mutableStateOf(false) }

    if (loading) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Sending request…", style = MaterialTheme.typography.bodySmall)
        }
    }

    error?.let {
        StatusBanner(describeError(it), isError = true)
    }
    notice?.let {
        if (error == null && response == null) StatusBanner(it, isError = false)
    }

    response?.let { res ->
        val statusColor = when {
            res.statusCode in 200..299 -> semantics.success
            res.statusCode in 300..399 -> semantics.info
            res.statusCode >= 400 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = statusColor.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    "${res.statusCode} ${res.statusMessage}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text("${res.durationMillis} ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(humanSize(res.sizeBytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (res.truncated) {
                Text("truncated", style = MaterialTheme.typography.labelSmall, color = semantics.warning)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = prettyJson, onClick = { prettyJson = !prettyJson }, label = { Text("Format JSON") })
            FilterChip(selected = showHeaders, onClick = { showHeaders = !showHeaders }, label = { Text("Headers (${res.headers.size})") })
        }
        val bodyText = if (prettyJson && res.contentType?.contains("json", true) == true) {
            val formatted = JsonTools.prettyPrint(res.body, 2)
            if (formatted is com.devkit.app.core.ToolResult.Success) formatted.value else res.body
        } else res.body
        OutputViewer(
            value = bodyText.ifEmpty { "(empty response body)" },
            label = "Response body",
            maxLines = 40,
            shareText = res.body,
        )
        if (showHeaders) {
            OutputViewer(
                value = res.headers.joinToString("\n") { "${it.first}: ${SensitiveData.redactHeaderValue(it.first, it.second)}" },
                label = "Response headers",
                maxLines = 30,
            )
        }
        Text(
            "Responses are treated as untrusted text. Dev Kit never executes returned JavaScript or HTML.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun describeError(error: ApiOutcome.NetworkError): String = when (error.kind) {
    ApiOutcome.NetworkError.Kind.TIMEOUT ->
        "${error.message} ${error.detail ?: "The server may be slow or unreachable."}"
    ApiOutcome.NetworkError.Kind.DNS ->
        "DNS lookup failed. Check the domain name and your network connection."
    ApiOutcome.NetworkError.Kind.TLS ->
        "TLS error. Dev Kit never disables certificate validation, so an invalid certificate fails here by design."
    ApiOutcome.NetworkError.Kind.CLEARTEXT_BLOCKED ->
        "Cleartext HTTP is blocked. Use HTTPS."
    ApiOutcome.NetworkError.Kind.CANCELLED -> "Request cancelled."
    ApiOutcome.NetworkError.Kind.INVALID_URL -> error.message
    else -> error.message
}

private fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

@Composable
private fun SaveRequestDialog(
    collections: List<CollectionEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var collectionId by remember { mutableStateOf(collections.firstOrNull()?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save request") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (collections.isNotEmpty()) {
                    Text("Collection", style = MaterialTheme.typography.labelMedium)
                    collections.forEach { collection ->
                        FilterChip(
                            selected = collectionId == collection.id,
                            onClick = { collectionId = collection.id },
                            label = { Text(collection.name) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Text(
                        "No collections yet. The request is saved without a collection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim(), collectionId) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun EnvironmentDialog(
    environments: List<EnvironmentEntity>,
    repository: DevKitRepository,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var variablesText by remember { mutableStateOf("base_url=https://api.example.com\ntoken=") }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Environments") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Use {{name}} in URL, headers or body. Values stay in this device's private database and are never exported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                environments.forEach { env ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(env.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { scope.launch { repository.deleteEnvironment(env.id) } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete environment")
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("New environment name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = variablesText,
                    onValueChange = { variablesText = it },
                    label = { Text("KEY=value per line") },
                    textStyle = CodeTextStyleSmall,
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val map = variablesText.lineSequence()
                            .mapNotNull { line ->
                                val idx = line.indexOf('=')
                                if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                            }
                            .toMap()
                        scope.launch { repository.saveEnvironment(name.trim(), map) }
                        name = ""
                    },
                ) { Text("Add environment") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
