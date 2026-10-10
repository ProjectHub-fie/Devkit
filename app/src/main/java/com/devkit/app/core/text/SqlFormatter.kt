package com.devkit.app.core.text

/**
 * Lightweight SQL formatter. It understands common statement keywords,
 * identifiers, string literals, and comments for the widely-used SQL dialects.
 * It is deliberately keyword based rather than a full grammar parser; this is
 * documented in the tool UI so users are not misled about dialect coverage.
 */
object SqlFormatter {

    private val KEYWORDS = setOf(
        "SELECT", "FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET",
        "INSERT INTO", "INSERT", "VALUES", "UPDATE", "SET", "DELETE FROM", "DELETE",
        "CREATE TABLE", "CREATE", "ALTER TABLE", "ALTER", "DROP TABLE", "DROP",
        "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "CROSS JOIN", "OUTER JOIN",
        "LEFT OUTER JOIN", "RIGHT OUTER JOIN", "FULL OUTER JOIN",
        "ON", "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE", "IS NULL", "IS NOT NULL",
        "UNION ALL", "UNION", "INTERSECT", "EXCEPT", "AS", "CASE", "WHEN", "THEN", "ELSE", "END",
        "DISTINCT", "ASC", "DESC", "COUNT", "SUM", "AVG", "MIN", "MAX", "RETURNING", "WITH"
    )

    private val MAJOR_CLAUSES = setOf(
        "SELECT", "FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET",
        "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM", "RETURNING", "UNION", "UNION ALL"
    )

    private val NEWLINE_BEFORE = setOf(
        "FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "LIMIT", "OFFSET",
        "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "CROSS JOIN",
        "LEFT OUTER JOIN", "RIGHT OUTER JOIN", "FULL OUTER JOIN",
        "UNION", "UNION ALL", "AND", "OR", "VALUES", "SET", "RETURNING"
    )

    fun format(input: String, upperCaseKeywords: Boolean = true): String {
        if (input.isBlank()) return ""
        val tokens = tokenize(input, upperCaseKeywords)
        val builder = StringBuilder()
        var indent = 0
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            val upper = token.uppercase()

            // Merge two-word compounds for clause detection.
            val combined = if (i + 1 < tokens.size) "$upper ${tokens[i + 1].uppercase()}" else ""
            val isCompound = combined in KEYWORDS && combined in MAJOR_CLAUSES ||
                combined in NEWLINE_BEFORE

            if (token == "(") {
                appendToken(builder, "(")
                indent++
                i++
                continue
            }
            if (token == ")") {
                indent = (indent - 1).coerceAtLeast(0)
                appendToken(builder, ")")
                i++
                continue
            }
            if (token == ",") {
                builder.append(",")
                newline(builder, indent)
                i++
                continue
            }
            if (token == ";") {
                builder.append(";")
                newline(builder, 0)
                i++
                continue
            }
            if (upper in NEWLINE_BEFORE && builder.isNotBlank()) {
                newline(builder, indent)
            }
            if (isCompound) {
                appendToken(builder, "$token ${tokens[i + 1]}")
                i += 2
                continue
            }
            appendToken(builder, token)
            i++
        }
        return builder.toString().trim()
    }

    private fun appendToken(builder: StringBuilder, token: String) {
        if (builder.isEmpty()) {
            builder.append(token)
            return
        }
        val last = builder.last()
        if (last != ' ' && last != '\n' && last != '(') builder.append(' ')
        builder.append(token)
    }

    private fun newline(builder: StringBuilder, indent: Int) {
        builder.append('\n')
        repeat(indent) { builder.append("  ") }
    }

    private fun tokenize(input: String, upper: Boolean): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        fun flush() {
            if (current.isNotEmpty()) {
                val word = current.toString()
                tokens.add(if (upper && word.uppercase() in KEYWORDS) word.uppercase() else word)
                current.clear()
            }
        }
        while (i < input.length) {
            val c = input[i]
            when {
                c == '\'' -> {
                    flush()
                    val sb = StringBuilder("'")
                    i++
                    while (i < input.length) {
                        sb.append(input[i])
                        if (input[i] == '\'' && (i + 1 >= input.length || input[i + 1] != '\'')) {
                            i++
                            break
                        }
                        if (input[i] == '\'' && i + 1 < input.length && input[i + 1] == '\'') {
                            sb.append(input[i + 1])
                            i += 2
                            continue
                        }
                        i++
                    }
                    tokens.add(sb.toString())
                }
                c == '-' && i + 1 < input.length && input[i + 1] == '-' -> {
                    flush()
                    val end = input.indexOf('\n', i).let { if (it == -1) input.length else it }
                    tokens.add(input.substring(i, end).trim())
                    i = end
                }
                c == '/' && i + 1 < input.length && input[i + 1] == '*' -> {
                    flush()
                    val end = input.indexOf("*/", i + 2).let { if (it == -1) input.length else it + 2 }
                    tokens.add(input.substring(i, end))
                    i = end
                }
                c.isWhitespace() -> {
                    flush()
                    i++
                }
                c in "(),;" -> {
                    flush()
                    tokens.add(c.toString())
                    i++
                }
                else -> {
                    current.append(c)
                    i++
                }
            }
        }
        flush()
        return tokens
    }
}
