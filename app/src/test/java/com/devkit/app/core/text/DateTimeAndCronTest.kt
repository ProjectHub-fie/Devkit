package com.devkit.app.core.text

import com.devkit.app.core.ToolResult
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeAndCronTest {

    private val utc = ZoneId.of("UTC")

    @Test
    fun `epoch seconds are auto-detected`() {
        val conversion = DateTimeTools.fromEpoch(1_700_000_000L, utc)
        assertEquals("2023-11-14T22:13:20Z", conversion.isoUtc)
        assertFalse(conversion.ambiguous)
    }

    @Test
    fun `epoch milliseconds are auto-detected`() {
        val conversion = DateTimeTools.fromEpoch(1_700_000_000_000L, utc)
        assertEquals(1_700_000_000L, conversion.seconds)
    }

    @Test
    fun `seconds and milliseconds of the same instant agree`() {
        val seconds = DateTimeTools.fromEpoch(1_700_000_000L, utc, DateTimeTools.Unit.SECONDS)
        val millis = DateTimeTools.fromEpoch(1_700_000_000_000L, utc, DateTimeTools.Unit.MILLISECONDS)
        assertEquals(seconds.epochMillis, millis.epochMillis)
    }

    @Test
    fun `negative epoch is reported as ambiguous rather than silent`() {
        val conversion = DateTimeTools.fromEpoch(-5L, utc)
        assertTrue(conversion.ambiguous)
    }

    @Test
    fun `parse epoch rejects non-numeric input`() {
        assertTrue(DateTimeTools.parseEpoch("tomorrow", utc) is ToolResult.Failure)
    }

    @Test
    fun `iso date string converts to a timestamp`() {
        val result = DateTimeTools.fromDateString("2024-02-29T12:30:00Z", utc)
        assertTrue(result is ToolResult.Success)
        assertEquals("2024-02-29T12:30:00Z", (result as ToolResult.Success).value.isoUtc)
    }

    @Test
    fun `invalid date string explains the expected formats`() {
        val result = DateTimeTools.fromDateString("29/02/2024", utc)
        assertTrue(result is ToolResult.Failure)
        assertTrue((result as ToolResult.Failure).message.contains("ISO-8601"))
    }

    @Test
    fun `timezone changes local output but not utc`() {
        val inUtc = DateTimeTools.fromEpoch(1_700_000_000L, ZoneId.of("UTC"))
        val inTokyo = DateTimeTools.fromEpoch(1_700_000_000L, ZoneId.of("Asia/Tokyo"))
        assertEquals(inUtc.isoUtc, inTokyo.isoUtc)
        assertTrue(inUtc.isoLocal != inTokyo.isoLocal)
    }

    @Test
    fun `cron validator accepts standard five field expressions`() {
        val analysis = CronTools.analyze("*/15 9-17 * * 1-5")
        assertTrue(analysis.valid)
        assertEquals(5, analysis.fields.size)
    }

    @Test
    fun `cron validator expands aliases`() {
        val analysis = CronTools.analyze("@daily")
        assertTrue(analysis.valid)
    }

    @Test
    fun `cron validator rejects wrong field count`() {
        val analysis = CronTools.analyze("* * *")
        assertFalse(analysis.valid)
    }

    @Test
    fun `cron validator rejects out of range values`() {
        val analysis = CronTools.analyze("99 * * * *")
        assertFalse(analysis.valid)
    }
}
