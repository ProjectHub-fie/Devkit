package com.devkit.app.data.repo

import com.devkit.app.core.web.CollectionDto
import com.devkit.app.core.web.CollectionExport
import com.devkit.app.core.web.HeaderDto
import com.devkit.app.core.web.ParamDto
import com.devkit.app.core.web.SavedRequestDto
import com.devkit.app.core.web.SensitiveData
import com.devkit.app.data.local.CollectionEntity
import com.devkit.app.data.local.DevKitDatabase
import com.devkit.app.data.local.EnvironmentEntity
import com.devkit.app.data.local.FavoriteToolEntity
import com.devkit.app.data.local.RequestHistoryEntity
import com.devkit.app.data.local.SavedRequestEntity
import com.devkit.app.data.local.SnippetEntity
import com.devkit.app.data.local.ToolHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Single gateway to local persistence. Nothing outside this class touches the
 * DAO, which keeps redaction and validation rules in one place.
 */
class DevKitRepository(db: DevKitDatabase) {

    private val dao = db.toolDao()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    // ----- History & favorites -----

    val toolHistory: Flow<List<ToolHistoryEntity>> = dao.history()
    val favorites: Flow<List<FavoriteToolEntity>> = dao.favorites()
    val snippets: Flow<List<SnippetEntity>> = dao.snippets()
    val collections: Flow<List<CollectionEntity>> = dao.collections()
    val savedRequests: Flow<List<SavedRequestEntity>> = dao.savedRequests()
    val environments: Flow<List<EnvironmentEntity>> = dao.environments()

    suspend fun recordToolUsage(toolId: String) =
        dao.recordUsage(ToolHistoryEntity(toolId = toolId, usedAt = System.currentTimeMillis()))

    suspend fun toggleFavorite(toolId: String, favorite: Boolean) {
        if (favorite) dao.upsertFavorite(FavoriteToolEntity(toolId, false, System.currentTimeMillis()))
        else dao.deleteFavorite(toolId)
    }

    suspend fun setPinned(toolId: String, pinned: Boolean) {
        dao.upsertFavorite(FavoriteToolEntity(toolId, pinned, System.currentTimeMillis()))
    }

    suspend fun clearToolHistory() = dao.clearHistory()

    // ----- Snippets -----

    fun searchSnippets(query: String): Flow<List<SnippetEntity>> = dao.searchSnippets(query)

    suspend fun saveSnippet(title: String, language: String, code: String): Long {
        val now = System.currentTimeMillis()
        return dao.upsertSnippet(
            SnippetEntity(title = title, language = language, code = code, createdAt = now, updatedAt = now)
        )
    }

    suspend fun deleteSnippet(id: Long) = dao.deleteSnippet(id)

    // ----- Collections & requests -----

    suspend fun saveCollection(name: String): Long {
        val now = System.currentTimeMillis()
        return dao.upsertCollection(CollectionEntity(name = name, createdAt = now, updatedAt = now))
    }

    suspend fun deleteCollection(id: Long) = dao.deleteCollection(id)

    fun requestsInCollection(id: Long): Flow<List<SavedRequestEntity>> = dao.savedRequestsInCollection(id)

    fun searchSavedRequests(query: String): Flow<List<SavedRequestEntity>> = dao.searchSavedRequests(query)

    suspend fun savedRequest(id: Long): SavedRequestEntity? = dao.savedRequest(id)

    /**
     * Persists a request. The raw URL is kept (needed to send it) but a redacted
     * copy is stored for list display so tokens never appear in the UI.
     */
    suspend fun saveRequest(
        id: Long? = null,
        collectionId: Long?,
        name: String,
        method: String,
        url: String,
        headers: List<Pair<String, String>>,
        queryParams: List<Pair<String, String>>,
        bodyType: String,
        body: String,
        timeoutSeconds: Int,
    ): Long {
        val now = System.currentTimeMillis()
        val headersJson = json.encodeToString(HeaderListSerializer, headers.map { HeaderDto(it.first, it.second) })
        val paramsJson = json.encodeToString(ParamListSerializer, queryParams.map { ParamDto(it.first, it.second) })
        return dao.upsertSavedRequest(
            SavedRequestEntity(
                id = id ?: 0,
                collectionId = collectionId,
                name = name,
                method = method,
                url = url,
                displayUrl = SensitiveData.redactUrl(url),
                headersJson = headersJson,
                queryParamsJson = paramsJson,
                bodyType = bodyType,
                body = body,
                timeoutSeconds = timeoutSeconds,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun deleteSavedRequest(id: Long) = dao.deleteSavedRequest(id)

    fun decodeHeaders(entity: SavedRequestEntity): List<HeaderDto> = try {
        json.decodeFromString(HeaderListSerializer, entity.headersJson)
    } catch (e: Exception) {
        emptyList()
    }

    fun decodeParams(entity: SavedRequestEntity): List<ParamDto> = try {
        json.decodeFromString(ParamListSerializer, entity.queryParamsJson)
    } catch (e: Exception) {
        emptyList()
    }

    // ----- Request history -----

    fun requestHistory(limit: Int = 100): Flow<List<RequestHistoryEntity>> = dao.requestHistory(limit)

    suspend fun recordRequest(method: String, displayUrl: String, statusCode: Int?, durationMillis: Long, label: String) {
        dao.recordRequest(
            RequestHistoryEntity(
                method = method,
                displayUrl = SensitiveData.redactUrl(displayUrl),
                statusCode = statusCode,
                durationMillis = durationMillis,
                outcomeLabel = label,
                executedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun clearRequestHistory() = dao.clearRequestHistory()

    /**
     * Applies the configured history retention. Called on app start so a
     * retention setting actually removes old rows instead of only being stored.
     */
    suspend fun pruneHistory(retentionDays: Int) {
        if (retentionDays <= 0) return
        val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L
        dao.deleteHistoryOlderThan(cutoff)
        dao.deleteRequestHistoryOlderThan(cutoff)
    }

    // ----- Environments -----

    suspend fun saveEnvironment(name: String, variables: Map<String, String>): Long {
        val obj = JsonObject(variables.mapValues { JsonPrimitive(it.value) })
        return dao.upsertEnvironment(
            EnvironmentEntity(name = name, variablesJson = json.encodeToString(JsonObject.serializer(), obj), createdAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteEnvironment(id: Long) = dao.deleteEnvironment(id)

    fun environmentVariables(entity: EnvironmentEntity): Map<String, String> = try {
        val obj = json.decodeFromString(JsonObject.serializer(), entity.variablesJson)
        obj.mapValues { it.value.toString().trim('"') }
    } catch (e: Exception) {
        emptyMap()
    }

    // ----- Export / import -----

    /** Redacts sensitive headers before exporting unless explicitly included. */
    suspend fun exportCollections(includeSecrets: Boolean = false): String {
        val allCollections = mutableListOf<CollectionDto>()
        // gather via flow first()
        val collectionsSnapshot = collections.first()
        for (collection in collectionsSnapshot) {
            val requests = requestsInCollection(collection.id).first()
            allCollections.add(
                CollectionDto(
                    name = collection.name,
                    requests = requests.map { entity ->
                        val headers = decodeHeaders(entity).map { header ->
                            if (!includeSecrets && SensitiveData.isSensitiveHeader(header.name)) {
                                header.copy(value = "")
                            } else header
                        }
                        SavedRequestDto(
                            name = entity.name,
                            method = entity.method,
                            url = if (includeSecrets) entity.url else SensitiveData.redactUrl(entity.url),
                            headers = headers,
                            queryParams = decodeParams(entity),
                            bodyType = entity.bodyType,
                            body = if (includeSecrets) entity.body else redactBodyIfSecret(entity.body),
                            timeoutSeconds = entity.timeoutSeconds,
                        )
                    }
                )
            )
        }
        return json.encodeToString(CollectionExport.serializer(), CollectionExport(collections = allCollections))
    }

    private fun redactBodyIfSecret(body: String): String =
        if (SensitiveData.looksLikeSecret(body)) "<redacted: appears to contain a secret>" else body

    /** Validates and imports an export file. Returns the number of requests imported. */
    suspend fun importCollections(payload: String): ImportResult {
        if (payload.isBlank()) return ImportResult.Failure("The selected file is empty.")
        if (payload.length > MAX_IMPORT_BYTES) return ImportResult.Failure("File is too large to import.")
        val parsed = try {
            json.decodeFromString(CollectionExport.serializer(), payload)
        } catch (e: Exception) {
            return ImportResult.Failure("Not a valid Dev Kit collection file: ${e.message?.take(120)}")
        }
        if (parsed.schema != "devkit.collection.v1") {
            return ImportResult.Failure("Unsupported schema '${parsed.schema}'.")
        }
        var count = 0
        for (collection in parsed.collections) {
            val id = saveCollection(collection.name)
            for (request in collection.requests) {
                if (request.url.isBlank() || request.url.length > MAX_URL_LENGTH) continue
                val method = request.method.uppercase().takeIf {
                    it in setOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")
                } ?: "GET"
                saveRequest(
                    collectionId = id,
                    name = request.name.take(200),
                    method = method,
                    url = request.url,
                    headers = request.headers.map { it.name to it.value },
                    queryParams = request.queryParams.map { it.name to it.value },
                    bodyType = request.bodyType,
                    body = request.body,
                    timeoutSeconds = request.timeoutSeconds.coerceIn(1, 120),
                )
                count++
            }
        }
        return ImportResult.Success(count)
    }

    suspend fun clearAllData() {
        dao.clearHistory()
        dao.clearRequestHistory()
        dao.wipeFavorites()
        dao.wipeSnippets()
        dao.wipeSavedRequests()
        dao.wipeCollections()
        dao.wipeEnvironments()
    }

    sealed interface ImportResult {
        data class Success(val count: Int) : ImportResult
        data class Failure(val message: String) : ImportResult
    }

    companion object {
        private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024
        private const val MAX_URL_LENGTH = 4096
        private val HeaderListSerializer =
            kotlinx.serialization.builtins.ListSerializer(HeaderDto.serializer())
        private val ParamListSerializer =
            kotlinx.serialization.builtins.ListSerializer(ParamDto.serializer())
    }
}
