package com.devkit.app.core.text

import com.devkit.app.core.ToolResult
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Timestamp conversion. Timestamps are handled in both seconds and
 * milliseconds. Ambiguous magnitudes (a value that is plausible as either) are
 * reported to the caller instead of being silently guessed.
 */
object DateTimeTools {

    data class Conversion(
        val epochMillis: Long,
        val seconds: Long,
        val isoUtc: String,
        val isoLocal: String,
        val rfc1123: String,
        val zoneId: String,
        val ambiguous: Boolean,
        val note: String?,
    )

    const val SECONDS_CUTOFF = 100_000_000_000L // 1e11: ms values are far larger

    fun fromEpoch(value: Long, zone: ZoneId, unit: Unit = Unit.AUTO): Conversion {
        val (millis, ambiguous, note) = when (unit) {
            Unit.SECONDS -> Triple(value * 1000, false, null)
            Unit.MILLISECONDS -> Triple(value, false, null)
            Unit.AUTO -> when {
                value in 0 until SECONDS_CUTOFF -> Triple(
                    value * 1000, false, "Interpreted as seconds."
                )
                value >= SECONDS_CUTOFF -> Triple(value, false, "Interpreted as milliseconds.")
                else -> Triple(value, true, "Negative value treated as milliseconds; verify the sign.")
            }
        }
        val instant = Instant.ofEpochMilli(millis)
        val zoned = ZonedDateTime.ofInstant(instant, zone)
        return Conversion(
            epochMillis = millis,
            seconds = millis / 1000,
            isoUtc = DateTimeFormatter.ISO_INSTANT.format(instant),
            isoLocal = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zoned),
            rfc1123 = DateTimeFormatter.RFC_1123_DATE_TIME.format(zoned),
            zoneId = zone.id,
            ambiguous = ambiguous,
            note = note,
        )
    }

    fun parseEpoch(input: String, zone: ZoneId, unit: Unit = Unit.AUTO): ToolResult<Conversion> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ToolResult.Failure("Enter a timestamp first.")
        val value = trimmed.toLongOrNull()
            ?: return ToolResult.Failure("'$trimmed' is not a numeric timestamp.")
        return ToolResult.Success(fromEpoch(value, zone, unit))
    }

    /** Accepts ISO-8601 / RFC-1123 / common date-time strings. */
    fun fromDateString(input: String, zone: ZoneId): ToolResult<Conversion> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ToolResult.Failure("Enter a date and time first.")
        val parsed: ZonedDateTime = try {
            when {
                trimmed.endsWith("Z") || trimmed.contains('T') || trimmed.contains('+') ->
                    ZonedDateTime.ofInstant(Instant.parse(trimmed), zone)
                else -> LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(zone)
            }
        } catch (e: DateTimeParseException) {
            return ToolResult.Failure(
                "Could not parse '$trimmed'. Use ISO-8601 (2024-01-31T12:00:00Z) or \"yyyy-MM-dd HH:mm:ss\"."
            )
        }
        return ToolResult.Success(
            fromEpoch(parsed.toInstant().toEpochMilli(), zone, Unit.MILLISECONDS)
        )
    }

    fun now(zone: ZoneId): Conversion = fromEpoch(System.currentTimeMillis(), zone, Unit.MILLISECONDS)

    enum class Unit { AUTO, SECONDS, MILLISECONDS }

    fun availableZones(): List<String> = ZoneId.getAvailableZoneIds().sorted()
}
