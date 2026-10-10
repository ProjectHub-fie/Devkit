package com.devkit.app.core.json

import com.devkit.app.core.ToolResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** CSV parsing and JSON conversion with RFC 4180 style quoting. */
object CsvTools {

    data class Table(val header: List<String>, val rows: List<List<String>>)

    fun parse(input: String, delimiter: Char = ','): ToolResult<Table> {
        if (input.isBlank()) return ToolResult.Failure("Paste CSV data first.")
        val rows = mutableListOf<List<String>>()
        val current = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < input.length && input[i + 1] == '"') {
                            field.append('"'); i++
                        } else inQuotes = false
                    } else field.append(c)
                }
                c == '"' -> inQuotes = true
                c == delimiter -> { current.add(field.toString()); field.clear() }
                c == '\n' -> {
                    current.add(field.toString()); field.clear()
                    rows.add(current.toList()); current.clear()
                }
                c == '\r' -> Unit
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || current.isNotEmpty()) {
            current.add(field.toString())
            rows.add(current.toList())
        }
        if (rows.isEmpty()) return ToolResult.Failure("No rows found.")
        val header = rows.first().map { it.trim() }
        val body = rows.drop(1).filter { it.any { cell -> cell.isNotBlank() } }
        return ToolResult.Success(Table(header, body))
    }

    fun toJson(input: String, delimiter: Char = ',', indent: Int = 2): ToolResult<String> {
        return when (val parsed = parse(input, delimiter)) {
            is ToolResult.Failure -> parsed
            is ToolResult.Success -> {
                val table = parsed.value
                val array = JsonArray(table.rows.map { row ->
                    JsonObject(
                        table.header.mapIndexed { index, key ->
                            key.ifBlank { "column${index + 1}" } to JsonPrimitive(row.getOrElse(index) { "" })
                        }.toMap()
                    )
                })
                JsonTools.prettyPrint(
                    kotlinx.serialization.json.Json.encodeToString(JsonElement.serializer(), array), indent
                )
            }
        }
    }
}
