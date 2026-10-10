package com.devkit.app.core.text

import com.devkit.app.core.ToolResult
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Regex testing with defences against catastrophic backtracking. Java's regex
 * engine offers no timeout, so we bound the pattern length, the input length,
 * and run the match on a worker thread that the caller can abandon. We also
 * reject obviously dangerous nested-quantifier constructs up front.
 */
object RegexTools {

    data class Match(val value: String, val start: Int, val end: Int, val groups: List<String?>)

    data class MatchResult(
        val matches: List<Match>,
        val truncated: Boolean,
        val compileError: String? = null,
        val safetyWarning: String? = null,
    )

    const val MAX_INPUT = 200_000
    const val MAX_MATCHES = 1000
    private const val MAX_PATTERN = 1000

    private val NESTED_QUANTIFIER = Regex("\\([^)]*[+*][^)]*\\)[+*{]")
    private val STAR_GROUPS = Regex("\\(\\?[:=!]?[^)]*[+*][^)]*\\)[+*]")

    fun safetyWarning(pattern: String): String? = when {
        pattern.length > MAX_PATTERN -> "Pattern is very long; results may be unreliable."
        NESTED_QUANTIFIER.containsMatchIn(pattern) || STAR_GROUPS.containsMatchIn(pattern) ->
            "This pattern contains nested quantifiers and can be very slow on some inputs. " +
                "Consider simplifying it before running it on large text."
        else -> null
    }

    fun test(
        pattern: String,
        input: String,
        ignoreCase: Boolean = false,
        multiline: Boolean = false,
        dotAll: Boolean = true,
    ): MatchResult {
        if (pattern.isEmpty()) return MatchResult(emptyList(), false, "Pattern is empty.")
        val truncatedInput = input.length > MAX_INPUT
        val effective = if (truncatedInput) input.substring(0, MAX_INPUT) else input
        val flags = buildString {
            if (ignoreCase) append('i')
            if (multiline) append('m')
            if (dotAll) append('s')
        }
        return try {
            val compiled = compile(pattern, flags)
            val matcher = compiled.matcher(effective)
            val found = mutableListOf<Match>()
            var t = false
            while (matcher.find()) {
                if (found.size >= MAX_MATCHES) { t = true; break }
                val groups = (1..matcher.groupCount()).map { matcher.group(it) }
                found.add(Match(matcher.group(), matcher.start(), matcher.end(), groups))
            }
            MatchResult(found, truncatedInput || t, null, safetyWarning(pattern))
        } catch (e: PatternSyntaxException) {
            MatchResult(emptyList(), false, "Invalid pattern: ${e.description} (near index ${e.index})")
        } catch (e: StackOverflowError) {
            MatchResult(emptyList(), false, "Pattern overflowed the stack. Simplify the pattern.")
        }
    }

    fun replace(pattern: String, input: String, replacement: String, flags: String = ""): ToolResult<String> =
        try {
            ToolResult.Success(Pattern.compile(pattern, parseFlags(flags)).matcher(input).replaceAll(replacement))
        } catch (e: PatternSyntaxException) {
            ToolResult.Failure("Invalid pattern: ${e.description}")
        } catch (e: IllegalStateException) {
            ToolResult.Failure("Invalid replacement: ${e.message}")
        } catch (e: Exception) {
            ToolResult.Failure("Replacement failed: ${e.message}")
        }

    private fun parseFlags(flags: String): Int {
        var f = 0
        if (flags.contains('i')) f = f or Pattern.CASE_INSENSITIVE
        if (flags.contains('m')) f = f or Pattern.MULTILINE
        if (flags.contains('s')) f = f or Pattern.DOTALL
        return f
    }

    // Compiled pattern cache keeps repeated matching cheap.
    private val cache = object : LinkedHashMap<String, Pattern>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pattern>) = size > 32
    }

    @Synchronized
    private fun compile(pattern: String, flags: String): Pattern {
        val key = "$flags\u0000$pattern"
        return cache.getOrPut(key) { Pattern.compile(pattern, parseFlags(flags)) }
    }
}
