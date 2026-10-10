package com.devkit.app.core.json

import com.devkit.app.core.ToolResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonToolsTest {

    @Test
    fun `valid json passes validation`() {
        val result = JsonTools.validate("""{"a":1,"b":[true,null]}""")
        assertTrue(result is ToolResult.Success)
    }

    @Test
    fun `empty input fails with a helpful message`() {
        val result = JsonTools.validate("   ")
        assertTrue(result is ToolResult.Failure)
        assertTrue((result as ToolResult.Failure).message.isNotBlank())
    }

    @Test
    fun `invalid json reports a syntax error`() {
        val result = JsonTools.validate("""{"a": }""")
        assertTrue(result is ToolResult.Failure)
        val message = (result as ToolResult.Failure).message
        assertTrue(message.contains("Invalid") || message.contains("JSON"))
    }

    @Test
    fun `pretty print applies requested indentation`() {
        val result = JsonTools.prettyPrint("""{"a":1}""", 4)
        assertTrue(result is ToolResult.Success)
        val pretty = (result as ToolResult.Success).value
        assertEquals("{\n    \"a\": 1\n}", pretty)
    }

    @Test
    fun `pretty print never throws on malformed input`() {
        val result = JsonTools.prettyPrint("not json")
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun `minify removes insignificant whitespace`() {
        val result = JsonTools.minify("{\n  \"a\": 1,\n  \"b\": [1, 2]\n}")
        assertTrue(result is ToolResult.Success)
        assertEquals("""{"a":1,"b":[1,2]}""", (result as ToolResult.Success).value)
    }

    @Test
    fun `unicode and emoji survive round trip`() {
        val source = """{"text":"café 😀 中文"}"""
        val pretty = JsonTools.prettyPrint(source, 2)
        assertTrue(pretty is ToolResult.Success)
        assertTrue((pretty as ToolResult.Success).value.contains("😀"))
        assertTrue(pretty.value.contains("中文"))
    }

    @Test
    fun `parse tree builds nested structure`() {
        val result = JsonTools.parseTree("""{"user":{"roles":["a","b"]}}""")
        assertTrue(result is ToolResult.Success)
        val root = (result as ToolResult.Success).value as JsonNode.ObjectNode
        val user = root.entries.first { it.first == "user" }.second as JsonNode.ObjectNode
        val roles = user.entries.first { it.first == "roles" }.second as JsonNode.ArrayNode
        assertEquals(2, roles.items.size)
    }

    @Test
    fun `parse tree handles deep nesting without crashing`() {
        val depth = 200
        val json = "[".repeat(depth) + "1" + "]".repeat(depth)
        val result = JsonTools.parseTree(json)
        assertTrue(result is ToolResult.Success)
    }

    @Test
    fun `large array parses`() {
        val json = (1..5000).joinToString(prefix = "[", postfix = "]")
        val result = JsonTools.validate(json)
        assertTrue(result is ToolResult.Success)
    }
}
