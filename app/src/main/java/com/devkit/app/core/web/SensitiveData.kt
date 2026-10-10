package com.devkit.app.core.web

/**
 * Central place for deciding which request data is sensitive. Everything the
 * app logs, stores in history previews, or exports must pass through here so
 * credentials are never written to disk or shown in lists in the clear.
 */
object SensitiveData {

    private val SENSITIVE_HEADERS = setOf(
        "authorization",
        "proxy-authorization",
        "cookie",
        "set-cookie",
        "x-api-key",
        "api-key",
        "apikey",
        "x-auth-token",
        "x-access-token",
        "x-csrf-token",
        "private-token",
    )

    const val REDACTED = "•••••••• (hidden)"

    fun isSensitiveHeader(name: String): Boolean = name.trim().lowercase() in SENSITIVE_HEADERS

    fun redactHeaderValue(name: String, value: String): String =
        if (isSensitiveHeader(name)) REDACTED else value

    /**
     * Redacts sensitive headers and common token-like query parameters. Used
     * before persisting history and before exporting collections.
     */
    fun redactUrl(rawUrl: String): String {
        val queryIndex = rawUrl.indexOf('?')
        if (queryIndex < 0) return rawUrl
        val base = rawUrl.substring(0, queryIndex)
        val query = rawUrl.substring(queryIndex + 1)
        val redacted = query.split('&').joinToString("&") { pair ->
            val key = pair.substringBefore('=')
            if (key.lowercase() in SENSITIVE_QUERY_KEYS) "$key=$REDACTED" else pair
        }
        return "$base?$redacted"
    }

    private val SENSITIVE_QUERY_KEYS = setOf(
        "token", "access_token", "api_key", "apikey", "key", "secret", "password",
        "auth", "session", "sig", "signature",
    )

    /** Best-effort scanner used to warn before exporting raw bodies. */
    fun looksLikeSecret(value: String): Boolean {
        if (value.length < 16) return false
        val patterns = listOf(
            Regex("(?i)bearer\\s+[A-Za-z0-9._-]{10,}"),
            Regex("(?i)(api[_-]?key|secret|token|password)\\s*[:=]\\s*\\S{8,}"),
            Regex("AKIA[0-9A-Z]{16}"),
            Regex("ghp_[A-Za-z0-9]{20,}"),
            Regex("-----BEGIN [A-Z ]*PRIVATE KEY-----"),
        )
        return patterns.any { it.containsMatchIn(value) }
    }
}
