package com.devkit.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tool_history")
data class ToolHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolId: String,
    val usedAt: Long,
)

@Entity(tableName = "favorite_tools")
data class FavoriteToolEntity(
    @PrimaryKey val toolId: String,
    val pinned: Boolean = false,
    val addedAt: Long,
)

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val language: String,
    val code: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "request_collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "saved_requests")
data class SavedRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionId: Long?,
    val name: String,
    val method: String,
    val url: String,
    /** Redacted-by-default URL kept for safe list display. */
    val displayUrl: String,
    val headersJson: String,
    val queryParamsJson: String,
    val bodyType: String,
    val body: String,
    val timeoutSeconds: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "request_history")
data class RequestHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val method: String,
    val displayUrl: String,
    val statusCode: Int?,
    val durationMillis: Long,
    val outcomeLabel: String,
    val executedAt: Long,
)

@Entity(tableName = "environments")
data class EnvironmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** JSON object of name -> value pairs. Values may contain secrets, kept in the private DB. */
    val variablesJson: String,
    val createdAt: Long,
)
