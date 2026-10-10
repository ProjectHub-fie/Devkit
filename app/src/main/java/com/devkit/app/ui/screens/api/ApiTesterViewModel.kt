package com.devkit.app.ui.screens.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devkit.app.core.web.ApiClient
import com.devkit.app.core.web.ApiOutcome
import com.devkit.app.core.web.ApiRequest
import com.devkit.app.core.web.ApiResponse
import com.devkit.app.core.web.BodyType
import com.devkit.app.core.web.HeaderDto
import com.devkit.app.core.web.HttpMethod
import com.devkit.app.core.web.ParamDto
import com.devkit.app.core.web.SavedRequestDto
import com.devkit.app.core.web.SensitiveData
import com.devkit.app.data.local.CollectionEntity
import com.devkit.app.data.local.EnvironmentEntity
import com.devkit.app.data.local.RequestHistoryEntity
import com.devkit.app.data.local.SavedRequestEntity
import com.devkit.app.data.repo.DevKitRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditableHeader(val name: String = "", val value: String = "", val enabled: Boolean = true)

data class EditableParam(val name: String = "", val value: String = "", val enabled: Boolean = true)

data class ApiUiState(
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "https://httpbin.org/get",
    val headers: List<EditableHeader> = emptyList(),
    val params: List<EditableParam> = emptyList(),
    val bodyType: BodyType = BodyType.NONE,
    val body: String = "",
    val timeoutSeconds: Int = 30,
    val followRedirects: Boolean = true,
    val loading: Boolean = false,
    val response: ApiResponse? = null,
    val error: ApiOutcome.NetworkError? = null,
    val notice: String? = null,
    val currentRequestName: String? = null,
    val currentRequestId: Long? = null,
) {
    val responseHeaders: List<Pair<String, String>> get() = response?.headers.orEmpty()
}

/**
 * Owns the API tester request state. Security rules live here:
 * - secrets are redacted before history/notification display
 * - cross-origin sends with sensitive headers require explicit confirmation
 * - environment variables substitute into URL/headers/body
 */
class ApiTesterViewModel(
    private val repository: DevKitRepository,
) : ViewModel() {

    private val client = ApiClient()
    private var sendJob: Job? = null

    private val _state = MutableStateFlow(ApiUiState())
    val state: StateFlow<ApiUiState> = _state.asStateFlow()

    private val _collections = MutableStateFlow<List<CollectionEntity>>(emptyList())
    val collections: StateFlow<List<CollectionEntity>> = _collections.asStateFlow()
    private val _environments = MutableStateFlow<List<EnvironmentEntity>>(emptyList())
    val environments: StateFlow<List<EnvironmentEntity>> = _environments.asStateFlow()
    private val _history = MutableStateFlow<List<RequestHistoryEntity>>(emptyList())
    val history: StateFlow<List<RequestHistoryEntity>> = _history.asStateFlow()

    init {
        viewModelScope.launch { repository.collections.collect { _collections.value = it } }
        viewModelScope.launch { repository.environments.collect { _environments.value = it } }
        viewModelScope.launch { repository.requestHistory().collect { _history.value = it } }
    }

    // ----- Editing -----

    fun setMethod(method: HttpMethod) { _state.value = _state.value.copy(method = method) }

    fun setUrl(url: String) { _state.value = _state.value.copy(url = url) }

    fun setBodyType(type: BodyType) { _state.value = _state.value.copy(bodyType = type) }

    fun setBody(body: String) { _state.value = _state.value.copy(body = body) }

    fun setTimeout(seconds: Int) {
        _state.value = _state.value.copy(timeoutSeconds = seconds.coerceIn(1, 120))
    }

    fun setFollowRedirects(enabled: Boolean) {
        _state.value = _state.value.copy(followRedirects = enabled)
    }

    fun addHeader() {
        _state.value = _state.value.copy(headers = _state.value.headers + EditableHeader())
    }

    fun updateHeader(index: Int, header: EditableHeader) {
        val list = _state.value.headers.toMutableList()
        if (index in list.indices) list[index] = header
        _state.value = _state.value.copy(headers = list)
    }

    fun removeHeader(index: Int) {
        val list = _state.value.headers.toMutableList()
        if (index in list.indices) list.removeAt(index)
        _state.value = _state.value.copy(headers = list)
    }

    fun addParam() {
        _state.value = _state.value.copy(params = _state.value.params + EditableParam())
    }

    fun updateParam(index: Int, param: EditableParam) {
        val list = _state.value.params.toMutableList()
        if (index in list.indices) list[index] = param
        _state.value = _state.value.copy(params = list)
    }

    fun removeParam(index: Int) {
        val list = _state.value.params.toMutableList()
        if (index in list.indices) list.removeAt(index)
        _state.value = _state.value.copy(params = list)
    }

    fun clearResponse() {
        _state.value = _state.value.copy(response = null, error = null, notice = null)
    }

    /** Surfaces a transient message in the response area (import results, validation, etc.). */
    fun showNotice(message: String) {
        _state.value = _state.value.copy(notice = message)
    }

    /** Replaces the whole editor from a saved request. */
    fun loadRequest(entity: SavedRequestEntity) {
        val headers = repository.decodeHeaders(entity).map { EditableHeader(it.name, it.value, it.enabled) }
        val params = repository.decodeParams(entity).map { EditableParam(it.name, it.value, it.enabled) }
        _state.value = _state.value.copy(
            method = runCatching { HttpMethod.valueOf(entity.method) }.getOrDefault(HttpMethod.GET),
            url = entity.url,
            headers = headers,
            params = params,
            bodyType = runCatching { BodyType.valueOf(entity.bodyType) }.getOrDefault(BodyType.NONE),
            body = entity.body,
            timeoutSeconds = entity.timeoutSeconds,
            currentRequestName = entity.name,
            currentRequestId = entity.id,
            response = null,
            error = null,
            notice = null,
        )
    }

    // ----- Environment substitution -----

    suspend fun variablesFor(environmentId: Long?): Map<String, String> {
        if (environmentId == null) return emptyMap()
        val entity = repository.environments.first().firstOrNull { it.id == environmentId } ?: return emptyMap()
        return repository.environmentVariables(entity)
    }

    // ----- Sending -----

    /**
     * Builds the effective request, checking for a cross-origin leak of a
     * sensitive header first. Returns the request via [onReady]; the caller
     * (UI) can show a confirmation before [send] is invoked.
     */
    fun prepare(environmentId: Long?, onReady: (ApiRequest, String?) -> Unit) {
        viewModelScope.launch {
            val variables = variablesFor(environmentId)
            val current = _state.value
            val substitutedUrl = com.devkit.app.util.VariableSubstitution.apply(current.url, variables)
            val headers = current.headers.map { header ->
                com.devkit.app.util.VariableSubstitution.apply(header.value, variables).let { header.copy(value = it.output) }
            }
            val body = com.devkit.app.util.VariableSubstitution.apply(current.body, variables).output
            val request = ApiRequest(
                method = current.method,
                url = substitutedUrl.output,
                headers = headers.map {
                    com.devkit.app.core.web.RequestHeader(it.name.trim(), it.value, it.enabled)
                },
                queryParams = current.params.map {
                    com.devkit.app.core.web.QueryParam(it.name.trim(), it.value, it.enabled)
                },
                bodyType = current.bodyType,
                body = body,
                timeoutSeconds = current.timeoutSeconds,
                followRedirects = current.followRedirects,
            )
            val warning = crossOriginWarning(request)
            onReady(request, warning)
        }
    }

    private fun crossOriginWarning(request: ApiRequest): String? {
        val sensitive = request.headers.filter { it.enabled && SensitiveData.isSensitiveHeader(it.name) }
        if (sensitive.isEmpty()) return null
        return "This request includes ${sensitive.size} sensitive header(s) " +
            "(${sensitive.joinToString { it.name }}). They will be sent to the host in the URL. " +
            "Confirm the URL is the intended origin."
    }

    fun send(request: ApiRequest) {
        if (sendJob?.isActive == true) return
        _state.value = _state.value.copy(loading = true, response = null, error = null, notice = null)
        sendJob = viewModelScope.launch {
            val outcome = client.execute(request)
            when (outcome) {
                is ApiOutcome.Success -> {
                    _state.value = _state.value.copy(loading = false, response = outcome.response, error = null)
                    repository.recordRequest(
                        method = request.method.name,
                        displayUrl = SensitiveData.redactUrl(request.url),
                        statusCode = outcome.response.statusCode,
                        durationMillis = outcome.response.durationMillis,
                        label = "HTTP ${outcome.response.statusCode}",
                    )
                }
                is ApiOutcome.NetworkError -> {
                    _state.value = _state.value.copy(loading = false, error = outcome, response = null)
                    repository.recordRequest(
                        method = request.method.name,
                        displayUrl = SensitiveData.redactUrl(request.url),
                        statusCode = null,
                        durationMillis = 0,
                        label = outcome.kind.name,
                    )
                }
            }
        }
    }

    fun cancel() {
        client.cancel()
        sendJob?.cancel()
        _state.value = _state.value.copy(loading = false, error = null, notice = "Request cancelled.")
    }

    override fun onCleared() {
        client.cancel()
        super.onCleared()
    }
}
