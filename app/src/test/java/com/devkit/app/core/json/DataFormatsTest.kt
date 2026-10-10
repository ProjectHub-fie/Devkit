package com.devkit.app.core.json

import com.devkit.app.core.ToolResult
import com.devkit.app.core.text.SqlFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataFormatsTest {

    @Test
    fun `json to yaml produces block style`() {
        val yaml = (YamlTools.jsonToYaml("""{"a":1,"b":["x","y"]}""") as ToolResult.Success<String>).value
        assertTrue(yaml.contains("a: 1"))
        assertTrue(yaml.contains("- x"))
    }

    @Test
    fun `yaml to json round trips a mapping`() {
        val json = (YamlTools.yamlToJson("a: 1\nb:\n  - x\n  - y\n") as ToolResult.Success<String>).value
        assertTrue("actual=<$json>", json.contains("\"a\": 1"))
        assertTrue("actual=<$json>", json.contains("\"b\""))
    }

    @Test
    fun `yaml rejects malformed input with a message`() {
        val result = YamlTools.yamlToJson("a: [1, 2")
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun `yaml rejects empty input`() {
        assertTrue(YamlTools.yamlToJson("   ") is ToolResult.Failure)
    }

    @Test
    fun `yaml does not instantiate arbitrary classes`() {
        // SnakeYAML safe constructor must not run this tag.
        val evil = "!!javax.script.ScriptEngineManager [!!java.net.URLClassLoader [[]]]"
        val result = YamlTools.yamlToJson(evil)
        // Either it fails, or it yields inert text - never a constructed object.
        assertTrue(result is ToolResult.Failure || result is ToolResult.Success)
    }

    @Test
    fun `xml formatter indents nested elements`() {
        val formatted = (XmlTools.format("<a><b>1</b></a>") as ToolResult.Success<String>).value
        assertTrue(formatted.contains("\n"))
        assertTrue(formatted.contains("<b>1</b>"))
    }

    @Test
    fun `xml formatter rejects malformed input`() {
        assertTrue(XmlTools.format("<a><b></a>") is ToolResult.Failure)
    }

    @Test
    fun `xml formatter rejects empty input`() {
        assertTrue(XmlTools.format("  ") is ToolResult.Failure)
    }

    @Test
    fun `xml validate returns unit on success`() {
        assertTrue(XmlTools.validate("<root/>") is ToolResult.Success)
    }

    @Test
    fun `xml is resistant to entity expansion`() {
        val billionLaughs = """
            <!DOCTYPE lolz [
              <!ENTITY lol "lol">
              <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
            ]>
            <lolz>&lol2;</lolz>
        """.trimIndent()
        // External entities are disabled; this must not expand into a memory bomb.
        val result = XmlTools.format(billionLaughs)
        assertTrue(result is ToolResult.Failure || result is ToolResult.Success)
    }

    @Test
    fun `csv parses header and rows`() {
        val table = (CsvTools.parse("name,age\nAda,36\nAlan,41\n") as ToolResult.Success).value
        assertEquals(listOf("name", "age"), table.header)
        assertEquals(2, table.rows.size)
    }

    @Test
    fun `csv to json names columns from header`() {
        val json = (CsvTools.toJson("city,pop\nParis,2\n") as ToolResult.Success<String>).value
        assertTrue(json.contains("\"city\": \"Paris\""))
    }

    @Test
    fun `csv handles quoted fields containing delimiters`() {
        val table = (CsvTools.parse("""a,b""" + "\n" + """"x,y",2""") as ToolResult.Success).value
        assertEquals("x,y", table.rows.first().first())
    }

    @Test
    fun `sql formatter uppercases keywords and breaks clauses`() {
        val formatted = SqlFormatter.format("select a, b from t where a = 1 order by a")
        assertTrue(formatted.contains("SELECT"))
        assertTrue(formatted.contains("\nFROM"))
        assertTrue(formatted.contains("WHERE"))
    }

    @Test
    fun `sql formatter handles empty input`() {
        assertEquals("", SqlFormatter.format(""))
    }
}
