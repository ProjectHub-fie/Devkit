package com.devkit.app.core.text

import com.devkit.app.core.ToolResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Escapes/unescapes strings for common target languages. */
object StringEscape {

    enum class Target { JSON, JAVA, JAVASCRIPT, PYTHON, SQL, SHELL, REGEX }

    fun escape(input: String, target: Target): String = when (target) {
        Target.JSON -> Json.encodeToString(input)
        Target.JAVA -> javaEscape(input, unicode = false)
        Target.JAVASCRIPT -> javaEscape(input, unicode = false)
        Target.PYTHON -> javaEscape(input, unicode = false)
        Target.SQL -> input.replace("'", "''")
        Target.SHELL -> "'" + input.replace("'", "'\\''") + "'"
        Target.REGEX -> input.replace(Regex("[.*+?^${'$'}{}()|\\[\\]\\\\]"), "\\\\${'$'}0")
    }

    fun unescape(input: String, target: Target): ToolResult<String> = try {
        when (target) {
            Target.JSON -> ToolResult.Success(Json.decodeFromString<String>(input))
            Target.JAVA, Target.JAVASCRIPT, Target.PYTHON -> ToolResult.Success(javaUnescape(input))
            Target.SQL -> ToolResult.Success(input.replace("''", "'"))
            Target.SHELL -> ToolResult.Success(unquoteShell(input))
            Target.REGEX -> ToolResult.Success(input)
        }
    } catch (e: Exception) {
        ToolResult.Failure("Could not unescape: ${e.message}")
    }

    private fun javaEscape(input: String, unicode: Boolean): String {
        val sb = StringBuilder()
        for (c in input) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                else -> if (unicode && c.code > 127) sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun javaUnescape(input: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c != '\\' || i == input.length - 1) { sb.append(c); i++; continue }
            val next = input[i + 1]
            when (next) {
                'n' -> { sb.append('\n'); i += 2 }
                'r' -> { sb.append('\r'); i += 2 }
                't' -> { sb.append('\t'); i += 2 }
                'b' -> { sb.append('\b'); i += 2 }
                'f' -> { sb.append('\u000C'); i += 2 }
                '"' -> { sb.append('"'); i += 2 }
                '\'' -> { sb.append('\''); i += 2 }
                '\\' -> { sb.append('\\'); i += 2 }
                'u' -> {
                    if (i + 5 < input.length) {
                        val hex = input.substring(i + 2, i + 6)
                        val value = hex.toIntOrNull(16)
                        if (value != null) { sb.append(value.toChar()); i += 6 }
                        else { sb.append(c); i++ }
                    } else { sb.append(c); i++ }
                }
                else -> { sb.append(next); i += 2 }
            }
        }
        return sb.toString()
    }

    private fun unquoteShell(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length >= 2) {
            return trimmed.substring(1, trimmed.length - 1).replace("'\\''", "'")
        }
        return trimmed
    }
}
