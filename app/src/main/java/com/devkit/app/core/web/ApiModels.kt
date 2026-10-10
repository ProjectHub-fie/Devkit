package com.devkit.app.core.web

import kotlinx.serialization.Serializable

@Serializable
enum class HttpMethod { GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS }

enum class BodyType { NONE, JSON, FORM_URLENCODED, PLAIN_TEXT }

data class RequestHeader(val name: String, val value: String, val enabled: Boolean = true)

data class QueryParam(val name: String, val value: String, val enabled: Boolean = true)

data class ApiRequest(
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "",
    val headers: List<RequestHeader> = emptyList(),
    val queryParams: List<QueryParam> = emptyList(),
    val bodyType: BodyType = BodyType.NONE,
    val body: String = "",
    val timeoutSeconds: Int = 30,
    val followRedirects: Boolean = true,
)

data class ApiResponse(
    val statusCode: Int,
    val statusMessage: String,
    val headers: List<Pair<String, String>>,
    val body: String,
    val durationMillis: Long,
    val sizeBytes: Long,
    val contentType: String?,
    val truncated: Boolean,
)

sealed interface ApiOutcome {
    data class Success(val response: ApiResponse) : ApiOutcome
    data class NetworkError(val kind: Kind, val message: String, val detail: String? = null) : ApiOutcome {
        enum class Kind { TIMEOUT, DNS, TLS, CONNECT, UNKNOWN_HOST, CLEARTEXT_BLOCKED, CANCELLED, INVALID_URL, IO, TOO_LARGE }
    }
}

/** Structured, JSON-serializable representation for saved collections and export. */
@Serializable
data class SavedRequestDto(
    val name: String,
    val method: String,
    val url: String,
    val headers: List<HeaderDto> = emptyList(),
    val queryParams: List<ParamDto> = emptyList(),
    val bodyType: String = "NONE",
    val body: String = "",
    val timeoutSeconds: Int = 30,
)

@Serializable
data class HeaderDto(val name: String, val value: String, val enabled: Boolean = true)

@Serializable
data class ParamDto(val name: String, val value: String, val enabled: Boolean = true)

@Serializable
data class CollectionExport(
    val schema: String = "devkit.collection.v1",
    val exportedAt: Long = System.currentTimeMillis(),
    val collections: List<CollectionDto>,
)

@Serializable
data class CollectionDto(
    val name: String,
    val requests: List<SavedRequestDto>,
)
