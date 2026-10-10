package com.devkit.app.core.web

import com.devkit.app.core.ToolResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveDataTest {

    @Test
    fun `authorization is sensitive`() {
        assertTrue(SensitiveData.isSensitiveHeader("Authorization"))
        assertTrue(SensitiveData.isSensitiveHeader("authorization"))
        assertTrue(SensitiveData.isSensitiveHeader("x-api-key"))
        assertTrue(SensitiveData.isSensitiveHeader("Cookie"))
    }

    @Test
    fun `content type is not sensitive`() {
        assertFalse(SensitiveData.isSensitiveHeader("Content-Type"))
        assertFalse(SensitiveData.isSensitiveHeader("Accept"))
    }

    @Test
    fun `sensitive header values are redacted`() {
        assertEquals(SensitiveData.REDACTED, SensitiveData.redactHeaderValue("Authorization", "Bearer abc"))
        assertEquals("application/json", SensitiveData.redactHeaderValue("Content-Type", "application/json"))
    }

    @Test
    fun `redact url hides token query params`() {
        val redacted = SensitiveData.redactUrl("https://api.dev/v1?token=abc123&page=2")
        assertFalse(redacted.contains("abc123"))
        assertTrue(redacted.contains("page=2"))
        assertTrue(redacted.contains("token="))
    }

    @Test
    fun `redact url without query is unchanged`() {
        val url = "https://api.dev/v1/users"
        assertEquals(url, SensitiveData.redactUrl(url))
    }

    @Test
    fun `secret detector recognises common token shapes`() {
        assertTrue(SensitiveData.looksLikeSecret("Authorization: Bearer abcdefghijklmnop"))
        assertTrue(SensitiveData.looksLikeSecret("api_key = sk_live_abcdefghijkl"))
        assertTrue(SensitiveData.looksLikeSecret("ghp_abcdefghijklmnopqrstuvwx"))
        assertFalse(SensitiveData.looksLikeSecret("hello world"))
    }

    @Test
    fun `hash inspector identifies sha256`() {
        val hash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        val result = HashInspector.identify(hash) as ToolResult.Success
        assertTrue(result.value.any { it.algorithm == "SHA-256" })
    }

    @Test
    fun `hash inspector rejects non-hex input`() {
        assertTrue(HashInspector.identify("zzzz") is ToolResult.Failure)
    }

    @Test
    fun `hash inspector rejects unknown length`() {
        assertTrue(HashInspector.identify("abcd") is ToolResult.Failure)
    }

    @Test
    fun `hash comparison is case insensitive`() {
        assertTrue(HashInspector.compare("ABCDEF", "abcdef"))
        assertFalse(HashInspector.compare("abcdef", "abcdeg"))
    }

    @Test
    fun `url parser extracts components`() {
        val result = UrlParser.parse("https://user@host.dev:8080/path/to?x=1#frag")
        val components = (result as ToolResult.Success).value
        assertEquals("https", components.scheme)
        assertEquals("host.dev", components.host)
        assertEquals(8080, components.port)
        assertEquals("/path/to", components.path)
        assertEquals("frag", components.fragment)
        assertTrue(components.queryParams.contains("x" to "1"))
    }

    @Test
    fun `url parser rejects blank input`() {
        assertTrue(UrlParser.parse("   ") is ToolResult.Failure)
    }
}
