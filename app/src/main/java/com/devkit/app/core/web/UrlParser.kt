package com.devkit.app.core.web

import com.devkit.app.core.ToolResult
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Splits a URL into its components without making any network call. */
object UrlParser {

    data class Components(
        val scheme: String,
        val userInfo: String?,
        val host: String,
        val port: Int,
        val path: String,
        val queryParams: List<Pair<String, String>>,
        val fragment: String?,
    )

    fun parse(input: String): ToolResult<Components> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ToolResult.Failure("Enter a URL first.")
        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val url = withScheme.toHttpUrlOrNull()
            ?: return ToolResult.Failure("Could not parse that URL.")
        return ToolResult.Success(
            Components(
                scheme = url.scheme,
                userInfo = url.encodedUsername.takeIf { it.isNotEmpty() },
                host = url.host,
                port = url.port,
                path = url.encodedPath,
                queryParams = url.queryParameterNames.map { name -> name to (url.queryParameter(name) ?: "") },
                fragment = url.fragment,
            )
        )
    }
}
