package com.devkit.app.core.text

import com.devkit.app.core.ToolResult
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextToolsTest {

    @Test
    fun `word count handles empty input`() {
        val counts = TextStats.analyze("")
        assertEquals(0, counts.words)
        assertEquals(0, counts.characters)
        assertEquals(0, counts.lines)
    }

    @Test
    fun `word count counts paragraphs and lines`() {
        val text = "hello world\n\nsecond paragraph here"
        val counts = TextStats.analyze(text)
        assertEquals(5, counts.words)
        assertEquals(3, counts.lines)
        assertEquals(2, counts.paragraphs)
    }

    @Test
    fun `case converter produces naming styles`() {
        assertEquals("Hello World", CaseConverter.title("hello world"))
        assertEquals("helloWorld", CaseConverter.camel("hello world"))
        assertEquals("HelloWorld", CaseConverter.pascal("hello world"))
        assertEquals("hello_world", CaseConverter.snake("hello world"))
        assertEquals("hello-world", CaseConverter.kebab("hello world"))
        assertEquals("HELLO_WORLD", CaseConverter.constant("hello world"))
    }

    @Test
    fun `case converter handles mixed separators`() {
        assertEquals("myTestValueValue", CaseConverter.camel("my_test-value value"))
    }

    @Test
    fun `diff flags additions and removals`() {
        val result = DiffTools.diff("a\nb\nc", "a\nx\nc")
        assertEquals(1, result.added)
        assertEquals(1, result.removed)
    }

    @Test
    fun `diff of identical input has no changes`() {
        val result = DiffTools.diff("same\ntext", "same\ntext")
        assertEquals(0, result.added)
        assertEquals(0, result.removed)
    }

    @Test
    fun `diff handles empty inputs`() {
        val result = DiffTools.diff("", "")
        assertFalse(result.truncated)
    }

    @Test
    fun `diff truncates enormous input instead of exhausting memory`() {
        val big = (1..(DiffTools.MAX_LINES + 500)).joinToString("\n")
        val result = DiffTools.diff(big, "x")
        assertTrue(result.truncated)
    }

    @Test
    fun `regex tester finds matches and groups`() {
        val result = RegexTools.test("(\\w+)@(\\w+)\\.com", "a@b.com c@d.com")
        assertEquals(2, result.matches.size)
        assertEquals("a", result.matches.first().groups.first())
    }

    @Test
    fun `regex tester reports syntax errors`() {
        val result = RegexTools.test("([", "text")
        assertTrue(result.compileError != null)
        assertTrue(result.matches.isEmpty())
    }

    @Test
    fun `regex tester warns about nested quantifiers`() {
        val result = RegexTools.test("(a+)+", "aaaa")
        assertTrue(result.safetyWarning != null)
    }

    @Test
    fun `regex tester handles empty pattern`() {
        val result = RegexTools.test("", "text")
        assertEquals("Pattern is empty.", result.compileError)
    }

    @Test
    fun `lorem generates requested paragraph count`() {
        val text = LoremTools.generate(LoremTools.Unit.PARAGRAPHS, 3)
        val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
        assertEquals(3, paragraphs.size)
    }

    @Test
    fun `lorem word mode returns requested word count`() {
        val text = LoremTools.generate(LoremTools.Unit.WORDS, 10)
        assertEquals(10, text.trim().split(Regex("\\s+")).size)
    }

    @Test
    fun `color parses hex and rgb equivalence`() {
        val hex = (ColorTools.parse("#7C6BF2") as ToolResult.Success).value
        val rgb = (ColorTools.parse("rgb(124,107,242)") as ToolResult.Success).value
        assertEquals(hex, rgb)
    }

    @Test
    fun `color rejects out of range rgb`() {
        assertTrue(ColorTools.parse("rgb(300,0,0)") is ToolResult.Failure)
    }

    @Test
    fun `color hsl round trips approximately`() {
        val rgb = (ColorTools.parse("#3366CC") as ToolResult.Success).value
        val hsl = ColorTools.rgbToHsl(rgb)
        val back = ColorTools.hslToRgb(hsl)
        assertTrue(kotlin.math.abs(back.r - rgb.r) <= 2)
        assertTrue(kotlin.math.abs(back.g - rgb.g) <= 2)
        assertTrue(kotlin.math.abs(back.b - rgb.b) <= 2)
    }

    @Test
    fun `diff detects changed lines`() {
        val result = DiffTools.diff("one\ntwo", "one\ntwo\nthree")
        assertEquals(1, result.added)
        assertEquals(0, result.removed)
    }
}
