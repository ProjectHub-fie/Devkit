package com.devkit.app.core.generators

import com.devkit.app.core.ToolResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorToolsTest {

    @Test
    fun `uuid v4 matches canonical format`() {
        val uuid = GeneratorTools.uuid(GeneratorTools.UuidVersion.V4_RANDOM)
        assertTrue(uuid.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")))
    }

    @Test
    fun `uuid v7 matches version and variant`() {
        val uuid = GeneratorTools.uuid(GeneratorTools.UuidVersion.V7_TIME_ORDERED)
        assertTrue(uuid.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")))
    }

    @Test
    fun `batched uuids are unique`() {
        val result = GeneratorTools.uuids(50) as ToolResult.Success
        assertEquals(50, result.value.size)
        assertEquals(50, result.value.toSet().size)
    }

    @Test
    fun `uuid rejects out of range counts`() {
        assertTrue(GeneratorTools.uuids(0) is ToolResult.Failure)
        assertTrue(GeneratorTools.uuids(10_000) is ToolResult.Failure)
    }

    @Test
    fun `random string honours length and alphabet`() {
        val result = GeneratorTools.randomString(32, "abc") as ToolResult.Success
        assertEquals(32, result.value.length)
        assertTrue(result.value.all { it in "abc" })
    }

    @Test
    fun `random string excludes ambiguous characters on request`() {
        val result = GeneratorTools.randomString(64, "Il1O0o", excludeAmbiguous = true)
        // All characters are ambiguous, so the pool becomes empty and this must fail.
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun `random string rejects empty alphabet`() {
        assertTrue(GeneratorTools.randomString(10, "") is ToolResult.Failure)
    }

    @Test
    fun `password contains one character from each selected set`() {
        val result = GeneratorTools.password(16, true, true, true, true) as ToolResult.Success
        assertTrue(result.value.any { it.isLowerCase() })
        assertTrue(result.value.any { it.isUpperCase() })
        assertTrue(result.value.any { it.isDigit() })
        assertTrue(result.value.any { !it.isLetterOrDigit() })
    }

    @Test
    fun `password rejects length shorter than the number of sets`() {
        assertTrue(GeneratorTools.password(2, true, true, true, false) is ToolResult.Failure)
    }

    @Test
    fun `sha256 hash of a known value is stable`() {
        val result = GeneratorTools.hash("abc", GeneratorTools.HashAlgorithm.SHA256) as ToolResult.Success
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            result.value,
        )
    }

    @Test
    fun `md5 differs from sha256`() {
        val md5 = (GeneratorTools.hash("abc", GeneratorTools.HashAlgorithm.MD5) as ToolResult.Success).value
        val sha = (GeneratorTools.hash("abc", GeneratorTools.HashAlgorithm.SHA256) as ToolResult.Success).value
        assertNotEquals(md5, sha)
    }

    @Test
    fun `hmac changes with the key`() {
        val a = (GeneratorTools.hmac("msg", "key1", GeneratorTools.HashAlgorithm.SHA256) as ToolResult.Success).value
        val b = (GeneratorTools.hmac("msg", "key2", GeneratorTools.HashAlgorithm.SHA256) as ToolResult.Success).value
        assertNotEquals(a, b)
    }

    @Test
    fun `password strength labels extremes correctly`() {
        assertTrue(GeneratorTools.passwordStrength("a").label in setOf("Weak", "Fair"))
        assertTrue(GeneratorTools.passwordStrength("Tr0ub4dor&3xyzQW").label in setOf("Good", "Strong"))
    }
}
