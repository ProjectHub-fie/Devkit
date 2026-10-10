package com.devkit.app.core.web

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * Per-request HTTP execution. Design decisions:
 *
 * - TLS validation is never disabled. There is no "trust all certificates"
 *   option anywhere in the codebase on purpose.
 * - Cleartext (http://) requests are blocked; Android's network security config
 *   rejects them, and we surface a clear message rather than crashing.
 * - Response bodies are streamed and capped at [MAX_RESPONSE_BYTES] so a huge
 *   response cannot exhaust memory on a low-end device.
 * - The client is rebuilt per timeout setting so a user change takes effect
 *   immediately without leaking thread pools (OkHttp pools are shared).
 */
class ApiClient(
    private val maxResponseBytes: Long = MAX_RESPONSE_BYTES,
    /**
     * Cleartext is always blocked in production. Unit tests point this at the
     * loopback MockWebServer host so request/response handling can be exercised
     * without weakening TLS validation or shipping a cleartext exception.
     */
    private val cleartextTestHosts: Set<String> = emptySet(),
) {

    companion object {
        const val MAX_RESPONSE_BYTES = 5L * 1024 * 1024 // 5 MiB
        private const val MIN_TIMEOUT = 1
        private const val MAX_TIMEOUT = 120

        private val sharedDispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 32
            maxRequestsPerHost = 8
        }
    }

    private var currentCall: Call? = null

    private fun buildClient(timeoutSeconds: Int, followRedirects: Boolean): OkHttpClient {
        val timeout = timeoutSeconds.coerceIn(MIN_TIMEOUT, MAX_TIMEOUT).toLong()
        return OkHttpClient.Builder()
            .dispatcher(sharedDispatcher)
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .writeTimeout(timeout, TimeUnit.SECONDS)
            .callTimeout((timeout * 2).coerceAtMost(300), TimeUnit.SECONDS)
            .followRedirects(followRedirects)
            .followSslRedirects(followRedirects)
            .retryOnConnectionFailure(true)
            .build()
    }

    suspend fun execute(request: ApiRequest): ApiOutcome = withContext(Dispatchers.IO) {
        val validation = validateRequest(request)
        if (validation != null) return@withContext validation

        val url = buildUrl(request.url, request.queryParams)
            ?: return@withContext ApiOutcome.NetworkError(
                ApiOutcome.NetworkError.Kind.INVALID_URL,
                "That URL could not be parsed. Include a scheme such as https://."
            )

        if (url.scheme == "http" && url.host !in cleartextTestHosts) {
            return@withContext ApiOutcome.NetworkError(
                ApiOutcome.NetworkError.Kind.CLEARTEXT_BLOCKED,
                "Plain HTTP is blocked. Use https:// instead.",
                "Dev Kit refuses cleartext traffic to protect your data."
            )
        }

        val body = buildBody(request)
        val headers = Headers.Builder().apply {
            request.headers.filter { it.enabled && it.name.isNotBlank() }.forEach {
                runCatching { add(it.name.trim(), it.value) }
            }
            if (body != null && !request.headers.any { it.enabled && it.name.equals("Content-Type", true) }) {
                add("Content-Type", contentTypeFor(request.bodyType))
            }
            if (!request.headers.any { it.enabled && it.name.equals("Accept", true) }) {
                add("Accept", "*/*")
            }
        }.build()

        val okRequest = Request.Builder()
            .url(url)
            .method(request.method.name, body)
            .headers(headers)
            .header("User-Agent", "DevKit/1.0 (Android)")
            .build()

        val client = buildClient(request.timeoutSeconds, request.followRedirects)
        val call = client.newCall(okRequest)
        currentCall = call
        val started = System.currentTimeMillis()
        try {
            val response: Response = call.execute()
            response.use { readResponse(it, started) }
        } catch (e: SocketTimeoutException) {
            ApiOutcome.NetworkError(
                ApiOutcome.NetworkError.Kind.TIMEOUT,
                "The request timed out after ${request.timeoutSeconds}s.",
                "Increase the timeout in the request settings if the server is slow."
            )
        } catch (e: UnknownHostException) {
            ApiOutcome.NetworkError(
                ApiOutcome.NetworkError.Kind.DNS,
                "Could not resolve the host. Check the domain and your connection.",
                e.message
            )
        } catch (e: SSLException) {
            ApiOutcome.NetworkError(
                ApiOutcome.NetworkError.Kind.TLS,
                "TLS handshake failed. The certificate may be invalid or expired.",
                e.message
            )
        } catch (e: IOException) {
            if (call.isCanceled()) {
                ApiOutcome.NetworkError(ApiOutcome.NetworkError.Kind.CANCELLED, "Request cancelled.")
            } else {
                ApiOutcome.NetworkError(
                    ApiOutcome.NetworkError.Kind.IO,
                    "Could not connect: ${e.message ?: "network error"}",
                    null
                )
            }
        } catch (e: IllegalArgumentException) {
            ApiOutcome.NetworkError(
                ApiOutcome.NetworkError.Kind.INVALID_URL,
                "Invalid request: ${e.message}"
            )
        } finally {
            currentCall = null
        }
    }

    fun cancel() {
        currentCall?.cancel()
    }

    private fun validateRequest(request: ApiRequest): ApiOutcome? {
        if (request.url.isBlank()) {
            return ApiOutcome.NetworkError(
                ApiOutcome.NetworkError.Kind.INVALID_URL,
                "Enter a request URL first."
            )
        }
        if (request.method != HttpMethod.GET && request.method != HttpMethod.HEAD &&
            request.method != HttpMethod.OPTIONS && request.method != HttpMethod.DELETE &&
            request.bodyType != BodyType.NONE && request.body.isEmpty()
        ) {
            return ApiOutcome.NetworkError(
                ApiOutcome.NetworkError.Kind.INVALID_URL,
                "${request.method} usually needs a body, but it is empty."
            )
        }
        return null
    }

    internal fun buildUrl(base: String, params: List<QueryParam>): okhttp3.HttpUrl? {
        val trimmed = base.trim()
        val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            // If the user typed a bare host, assume https rather than silently using http.
            if (trimmed.contains("://")) trimmed else "https://$trimmed"
        } else trimmed
        val builder = withScheme.toHttpUrlOrNull()?.newBuilder() ?: return null
        params.filter { it.enabled && it.name.isNotBlank() }.forEach { param ->
            runCatching { builder.addQueryParameter(param.name, param.value) }
        }
        return builder.build()
    }

    private fun buildBody(request: ApiRequest): okhttp3.RequestBody? {
        val method = request.method
        val canHaveBody = method == HttpMethod.POST || method == HttpMethod.PUT ||
            method == HttpMethod.PATCH || method == HttpMethod.DELETE
        if (!canHaveBody || request.bodyType == BodyType.NONE) {
            return when (method) {
                HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH ->
                    ByteArray(0).toRequestBody(contentTypeFor(BodyType.NONE).toMediaTypeOrNull())
                else -> null
            }
        }
        return request.body.toRequestBody(contentTypeFor(request.bodyType).toMediaTypeOrNull())
    }

    private fun contentTypeFor(type: BodyType): String = when (type) {
        BodyType.JSON -> "application/json; charset=utf-8"
        BodyType.FORM_URLENCODED -> "application/x-www-form-urlencoded; charset=utf-8"
        BodyType.PLAIN_TEXT -> "text/plain; charset=utf-8"
        BodyType.NONE -> "application/octet-stream"
    }

    private fun readResponse(response: Response, started: Long): ApiOutcome {
        val body = response.body
        val contentType = response.header("Content-Type")
        if (body == null) {
            return ApiOutcome.Success(
                ApiResponse(
                    statusCode = response.code,
                    statusMessage = response.message,
                    headers = response.headers.toList(),
                    body = "",
                    durationMillis = System.currentTimeMillis() - started,
                    sizeBytes = 0,
                    contentType = contentType,
                    truncated = false,
                )
            )
        }
        val length = body.contentLength()
        if (length > maxResponseBytes) {
            return ApiOutcome.NetworkError(
                ApiOutcome.NetworkError.Kind.TOO_LARGE,
                "Response is ${humanSize(length)}, larger than the ${humanSize(maxResponseBytes)} limit.",
                "Dev Kit limits response size to protect memory. Use curl for very large downloads."
            )
        }
        val bytes = body.byteStream().use { stream -> readCapped(stream, maxResponseBytes) }
        val truncated = bytes.size >= maxResponseBytes
        val text = String(bytes, Charsets.UTF_8)
        return ApiOutcome.Success(
            ApiResponse(
                statusCode = response.code,
                statusMessage = response.message,
                headers = response.headers.toList(),
                body = text,
                durationMillis = System.currentTimeMillis() - started,
                sizeBytes = bytes.size.toLong(),
                contentType = contentType,
                truncated = truncated,
            )
        )
    }

    private fun readCapped(stream: java.io.InputStream, cap: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read == -1) break
            val remaining = (cap - total).coerceAtLeast(0)
            if (remaining == 0L) break
            val toWrite = minOf(read.toLong(), remaining).toInt()
            out.write(buffer, 0, toWrite)
            total += toWrite
            if (total >= cap) break
        }
        return out.toByteArray()
    }

    internal fun humanSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> "%.1f MiB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> "%.1f KiB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
