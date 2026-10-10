package com.devkit.app.core.text

import com.devkit.app.core.ToolResult
import com.devkit.app.core.web.JwtTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAndTextUtilsTest {

    @Test
    fun `json escape round trips through unescape`() {
        val original = """He said "hi" and\nleft\\now"""
        val escaped = StringEscape.escape(original, StringEscape.Target.JSON)
        val back = (StringEscape.unescape(escaped, StringEscape.Target.JSON) as ToolResult.Success<String>).value
        assertEquals(original, back)
    }

    @Test
    fun `sql escape doubles single quotes`() {
        assertEquals("O''Brien", StringEscape.escape("O'Brien", StringEscape.Target.SQL))
        assertEquals("O'Brien", (StringEscape.unescape("O''Brien", StringEscape.Target.SQL) as ToolResult.Success<String>).value)
    }

    @Test
    fun `shell escape wraps and escapes`() {
        val escaped = StringEscape.escape("it's", StringEscape.Target.SHELL)
        assertTrue(escaped.startsWith("'"))
        assertEquals("it's", (StringEscape.unescape(escaped, StringEscape.Target.SHELL) as ToolResult.Success<String>).value)
    }

    @Test
    fun `regex escape neutralises metacharacters`() {
        val escaped = StringEscape.escape("a.b*c", StringEscape.Target.REGEX)
        assertTrue(escaped.contains("\\."))
        assertTrue(escaped.contains("\\*"))
    }

    @Test
    fun `json unescape reports invalid escape sequences`() {
        assertTrue(StringEscape.unescape("\"\\x\"", StringEscape.Target.JSON) is ToolResult.Failure)
    }

    @Test
    fun `markdown renders headings to html`() {
        val html = MarkdownTools.toHtml("# Title\n\n- item")
        assertTrue(html.contains("<h1>Title</h1>"))
        assertTrue(html.contains("<li>item</li>"))
    }

    @Test
    fun `markdown escapes embedded raw html as text`() {
        val html = MarkdownTools.toHtml("hello <script>alert(1)</script>")
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
    }

    @Test
    fun `markdown of empty input is empty`() {
        assertEquals("", MarkdownTools.toHtml(""))
    }

    @Test
    fun `jwt decode splits a well-formed token`() {
        // header {"alg":"HS256","typ":"JWT"} . payload {"sub":"1","exp":4102444800} . signature
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxIiwiZXhwIjo0MTAyNDQ0ODAwfQ." +
            "c2lnbmF0dXJl"
        val decoded = (JwtTools.decode(token) as ToolResult.Success<JwtTools.Decoded>).value
        assertEquals("HS256", decoded.algorithm)
        assertTrue(decoded.signaturePresent)
        assertEquals(false, decoded.expired)
        assertTrue(decoded.payload.contains("\"sub\""))
    }

    @Test
    fun `jwt decode reports malformed tokens`() {
        assertTrue(JwtTools.decode("not-a-token") is ToolResult.Failure)
    }

    @Test
    fun `jwt decode flags expired tokens`() {
        val expiredPayload = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("""{"exp":1}""".toByteArray())
        val token = "eyJhbGciOiJIUzI1NiJ9.$expiredPayload.sig"
        val decoded = (JwtTools.decode(token) as ToolResult.Success<JwtTools.Decoded>).value
        assertEquals(true, decoded.expired)
    }
}
