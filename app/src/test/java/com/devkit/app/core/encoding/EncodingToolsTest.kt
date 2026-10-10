package com.devkit.app.core.encoding

import com.devkit.app.core.ToolResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncodingToolsTest {

    @Test
    fun `base64 encode decodes with UTF-8`() {
        val encoded = EncodingTools.base64Encode("héllo") as ToolResult.Success
        assertEquals("aMOpbGxv", encoded.value.standard)
        val decoded = EncodingTools.base64Decode(encoded.value.standard)
        assertEquals("héllo", (decoded as ToolResult.Success<String>).value)
    }

    @Test
    fun `url safe base64 differs from standard`() {
        val encoded = EncodingTools.base64Encode("subjects?999") as ToolResult.Success
        assertNotEquals(encoded.value.standard, encoded.value.urlSafe)
    }

    @Test
    fun `malformed base64 fails with explanation`() {
        val result = EncodingTools.base64Decode("not valid base64 !!!")
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun `base64 without padding still decodes`() {
        val result = EncodingTools.base64Decode("aMOpbGxv")
        assertTrue(result is ToolResult.Success)
    }

    @Test
    fun `url component encoding escapes ampersand`() {
        assertEquals("a%26b", EncodingTools.urlEncodeComponent("a&b"))
    }

    @Test
    fun `url component encoding escapes space as percent20`() {
        assertEquals("a%20b", EncodingTools.urlEncodeComponent("a b"))
    }

    @Test
    fun `full url encoding preserves structure`() {
        val encoded = EncodingTools.urlEncodeFull("https://x.dev/a b?q=c d")
        assertTrue("actual=<$encoded>", encoded.startsWith("https://x.dev/"))
        assertTrue("actual=<$encoded>", encoded.contains("?"))
        assertTrue("actual=<$encoded>", encoded.contains("%20"))
    }
    @Test
    fun `url decode reverses percent encoding`() {
        val result = EncodingTools.urlDecode("a%26b%20c")
        assertEquals("a&b c", (result as ToolResult.Success<String>).value)
    }

    @Test
    fun `html entity encode and decode round trip`() {
        val encoded = EncodingTools.htmlEncode("<b>café & tea</b>", encodeAll = false)
        assertTrue(encoded.contains("&lt;"))
        assertTrue(encoded.contains("&amp;"))
        val decoded = EncodingTools.htmlDecode(encoded)
        assertEquals("<b>café & tea</b>", (decoded as ToolResult.Success<String>).value)
    }

    @Test
    fun `html decode handles numeric entities`() {
        assertEquals("A", (EncodingTools.htmlDecode("&#65;") as ToolResult.Success<String>).value)
    }

    @Test
    fun `radix parses hex binary octal and decimal`() {
        assertEquals("255", (EncodingTools.parseAny("0xFF") as ToolResult.Success).value.decimal)
        assertEquals("255", (EncodingTools.parseAny("0b11111111") as ToolResult.Success).value.decimal)
        assertEquals("255", (EncodingTools.parseAny("0o377") as ToolResult.Success).value.decimal)
        assertEquals("255", (EncodingTools.parseAny("255") as ToolResult.Success).value.decimal)
    }

    @Test
    fun `radix reports invalid digits`() {
        assertTrue(EncodingTools.parseAny("0b12345") is ToolResult.Failure)
    }

    @Test
    fun `radix of empty input fails`() {
        assertTrue(EncodingTools.parseAny("  ") is ToolResult.Failure)
    }

    @Test
    fun `unicode inspector handles surrogate pairs`() {
        val info = EncodingTools.inspect("😀")
        assertEquals(1, info.size)
        assertEquals(0x1F600, info.first().codePoint)
    }

    @Test
    fun `unicode inspector reports utf8 bytes`() {
        val info = EncodingTools.inspect("é").first()
        assertEquals("C3 A9", info.utf8)
    }
}
