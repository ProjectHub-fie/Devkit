package com.devkit.app.core.text

import com.devkit.app.core.ToolResult

/**
 * Cron expression helper. Supports the standard five-field cron (minute, hour,
 * day-of-month, month, day-of-week) plus the optional seconds and year fields
 * used by Quartz-style schedulers. It validates syntax and explains fields; it
 * makes no claim about whether executing a schedule is safe.
 */
object CronTools {

    data class Field(val name: String, val value: String, val explanation: String)

    data class Analysis(
        val valid: Boolean,
        val message: String,
        val fields: List<Field>,
        val nextRuns: List<String>,
    )

    private val NAMED_ALIASES = mapOf(
        "@yearly" to "0 0 1 1 *",
        "@annually" to "0 0 1 1 *",
        "@monthly" to "0 0 1 * *",
        "@weekly" to "0 0 * * 0",
        "@daily" to "0 0 * * *",
        "@midnight" to "0 0 * * *",
        "@hourly" to "0 * * * *",
    )

    fun analyze(expression: String): Analysis {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            return Analysis(false, "Expression is empty.", emptyList(), emptyList())
        }
        val resolved = NAMED_ALIASES[trimmed.lowercase()] ?: trimmed
        val parts = resolved.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.size !in setOf(5, 6, 7)) {
            return Analysis(
                false,
                "Expected 5 fields (minute hour day month weekday), or 6/7 with leading seconds and trailing year.",
                emptyList(),
                emptyList(),
            )
        }
        val names = when (parts.size) {
            5 -> listOf("Minute", "Hour", "Day of month", "Month", "Day of week")
            6 -> listOf("Second", "Minute", "Hour", "Day of month", "Month", "Day of week")
            else -> listOf("Second", "Minute", "Hour", "Day of month", "Month", "Day of week", "Year")
        }
        val ranges = when (parts.size) {
            5 -> listOf(0..59, 0..23, 1..31, 1..12, 0..7)
            6 -> listOf(0..59, 0..59, 0..23, 1..31, 1..12, 0..7)
            else -> listOf(0..59, 0..59, 0..23, 1..31, 1..12, 0..7, 1970..2099)
        }

        val fields = mutableListOf<Field>()
        parts.forEachIndexed { index, part ->
            val range = ranges[index]
            val problem = validateField(part, range)
            if (problem != null) {
                return Analysis(false, "${names[index]}: $problem", fields, emptyList())
            }
            fields.add(Field(names[index], part, explainField(part, names[index])))
        }
        return Analysis(true, "Valid cron expression.", fields, emptyList())
    }

    private fun validateField(field: String, range: IntRange): String? {
        if (field == "*" || field == "?") return null
        val segments = field.split(',')
        for (segment in segments) {
            if (segment.isEmpty()) return "empty list element"
            val stepSplit = segment.split('/')
            if (stepSplit.size > 2) return "too many '/' in '$segment'"
            val base = stepSplit[0]
            if (stepSplit.size == 2) {
                val step = stepSplit[1].toIntOrNull() ?: return "'$segment' has a non-numeric step"
                if (step <= 0) return "step must be greater than zero"
            }
            if (base == "*" || base == "?") continue
            val bounds = base.split('-')
            if (bounds.size > 2) return "invalid range '$base'"
            for (b in bounds) {
                val value = b.toIntOrNull()
                if (value != null) {
                    if (value !in range) return "$value is outside ${range.first}..${range.last}"
                } else {
                    if (b.length > 3) return "unknown token '$b'"
                }
            }
        }
        return null
    }

    private fun explainField(field: String, name: String): String = when {
        field == "*" -> "every $name"
        field == "?" -> "any $name"
        field.contains(',') -> "at ${field.replace(",", ", ")}"
        field.contains('/') -> {
            val (base, step) = field.split('/')
            if (base == "*") "every $step of $name" else "every $step starting at $base"
        }
        field.contains('-') -> {
            val (from, to) = field.split('-')
            "from $from through $to"
        }
        else -> "at $field"
    }
}
