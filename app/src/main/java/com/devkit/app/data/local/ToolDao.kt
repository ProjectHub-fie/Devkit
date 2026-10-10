package com.devkit.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolDao {
    @Query("SELECT * FROM tool_history ORDER BY usedAt DESC")
    fun history(): Flow<List<ToolHistoryEntity>>

    @Insert
    suspend fun recordUsage(entry: ToolHistoryEntity)

    @Query("DELETE FROM tool_history")
    suspend fun clearHistory()

    @Query("DELETE FROM tool_history")
    suspend fun wipeToolHistory()

    @Query("DELETE FROM request_history")
    suspend fun wipeRequestHistory()

    @Query("DELETE FROM tool_history WHERE usedAt < :cutoff")
    suspend fun deleteHistoryOlderThan(cutoff: Long)

    @Query("SELECT * FROM favorite_tools ORDER BY addedAt DESC")
    fun favorites(): Flow<List<FavoriteToolEntity>>

    @Upsert
    suspend fun upsertFavorite(favorite: FavoriteToolEntity)

    @Query("DELETE FROM favorite_tools WHERE toolId = :toolId")
    suspend fun deleteFavorite(toolId: String)

    // ----- Snippets -----

    @Query("SELECT * FROM snippets ORDER BY updatedAt DESC")
    fun snippets(): Flow<List<SnippetEntity>>

    @Query("SELECT * FROM snippets WHERE title LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchSnippets(query: String): Flow<List<SnippetEntity>>

    @Upsert
    suspend fun upsertSnippet(snippet: SnippetEntity): Long

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteSnippet(id: Long)

    // ----- Collections & requests -----

    @Query("SELECT * FROM request_collections ORDER BY name COLLATE NOCASE")
    fun collections(): Flow<List<CollectionEntity>>

    @Upsert
    suspend fun upsertCollection(collection: CollectionEntity): Long

    @Query("DELETE FROM request_collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("SELECT * FROM saved_requests ORDER BY updatedAt DESC")
    fun savedRequests(): Flow<List<SavedRequestEntity>>

    @Query("SELECT * FROM saved_requests WHERE collectionId = :collectionId ORDER BY updatedAt DESC")
    fun savedRequestsInCollection(collectionId: Long): Flow<List<SavedRequestEntity>>

    @Query("SELECT * FROM saved_requests WHERE name LIKE '%' || :query || '%' OR displayUrl LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchSavedRequests(query: String): Flow<List<SavedRequestEntity>>

    @Query("SELECT * FROM saved_requests WHERE id = :id")
    suspend fun savedRequest(id: Long): SavedRequestEntity?

    @Upsert
    suspend fun upsertSavedRequest(request: SavedRequestEntity): Long

    @Query("DELETE FROM saved_requests WHERE id = :id")
    suspend fun deleteSavedRequest(id: Long)

    // ----- Request history -----

    @Query("SELECT * FROM request_history ORDER BY executedAt DESC LIMIT :limit")
    fun requestHistory(limit: Int): Flow<List<RequestHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordRequest(entry: RequestHistoryEntity)

    @Query("DELETE FROM request_history")
    suspend fun clearRequestHistory()

    @Query("DELETE FROM request_history WHERE executedAt < :cutoff")
    suspend fun deleteRequestHistoryOlderThan(cutoff: Long)

    // ----- Environments -----

    @Query("SELECT * FROM environments ORDER BY name COLLATE NOCASE")
    fun environments(): Flow<List<EnvironmentEntity>>

    @Upsert
    suspend fun upsertEnvironment(environment: EnvironmentEntity): Long

    @Query("DELETE FROM environments WHERE id = :id")
    suspend fun deleteEnvironment(id: Long)

    // ----- Bulk wipe (used by "clear all data") -----

    @Query("DELETE FROM favorite_tools")
    suspend fun wipeFavorites()

    @Query("DELETE FROM snippets")
    suspend fun wipeSnippets()

    @Query("DELETE FROM saved_requests")
    suspend fun wipeSavedRequests()

    @Query("DELETE FROM request_collections")
    suspend fun wipeCollections()

    @Query("DELETE FROM environments")
    suspend fun wipeEnvironments()
}
