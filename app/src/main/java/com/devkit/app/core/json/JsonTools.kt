package com.devkit.app.core.json

import com.devkit.app.core.ToolResult
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Pure JSON processing. All functions are CPU bound and are expected to be
 * invoked from a background dispatcher by the ViewModel layer.
 */
object JsonTools {

    private fun jsonFor(indent: Int): Json = Json {
        prettyPrint = true
        prettyPrintIndent = " ".repeat(indent.coerceIn(0, 8).coerceAtLeast(1))
        isLenient = false
        ignoreUnknownKeys = true
    }

    /** Parses and reports the first syntax error with position context. */
    fun validate(input: String): ToolResult<JsonElement> {
        if (input.isBlank()) return ToolResult.Failure("Input is empty. Paste some JSON first.")
        return try {
            ToolResult.Success(Json.parseToJsonElement(input))
        } catch (e: SerializationException) {
            ToolResult.Failure(describe(e, input))
        } catch (e: IllegalArgumentException) {
            ToolResult.Failure(describe(e, input))
        }
    }

    fun prettyPrint(input: String, indent: Int = 2): ToolResult<String> {
        if (input.isBlank()) return ToolResult.Failure("Input is empty. Paste some JSON first.")
        return try {
            val element = Json.parseToJsonElement(input)
            ToolResult.Success(jsonFor(indent).encodeToString(JsonElement.serializer(), element))
        } catch (e: Exception) {
            ToolResult.Failure(describe(e, input))
        }
    }

    fun minify(input: String): ToolResult<String> {
        if (input.isBlank()) return ToolResult.Failure("Input is empty. Paste some JSON first.")
        return try {
            val element = Json.parseToJsonElement(input)
            ToolResult.Success(Json.encodeToString(JsonElement.serializer(), element))
        } catch (e: Exception) {
            ToolResult.Failure(describe(e, input))
        }
    }

    /**
     * Builds a readable path -> summary map for the viewer. Kept shallow on
     * purpose so very large documents stay responsive.
     */
    fun buildTree(element: JsonElement): JsonNode = when (element) {
        is JsonObject -> JsonNode.ObjectNode(
            element.entries.map { (k, v) -> k to buildTree(v) }
        )
        is JsonArray -> JsonNode.ArrayNode(element.map { buildTree(it) })
        is JsonNull -> JsonNode.NullNode
        is JsonPrimitive -> JsonNode.ValueNode(element)
    }

    fun parseTree(input: String): ToolResult<JsonNode> =
        validate(input).let { result ->
            when (result) {
                is ToolResult.Success -> ToolResult.Success(buildTree(result.value))
                is ToolResult.Failure -> result
            }
        }

    private fun describe(e: Exception, input: String): String {
        val raw = e.message ?: "Invalid JSON"
        val offset = RAW_OFFSET.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (offset == null) return "Invalid JSON: $raw"
        val (line, col) = lineAndColumn(input, offset)
        return "Invalid JSON at line $line, column $col: ${raw.substringBefore(" at path")}"
    }

    private fun lineAndColumn(text: String, offset: Int): Pair<Int, Int> {
        val safeOffset = offset.coerceIn(0, text.length)
        var line = 1
        var lastLineStart = 0
        for (i in 0 until safeOffset) {
            if (text[i] == '\n') {
                line++
                lastLineStart = i + 1
            }
        }
        return line to (safeOffset - lastLineStart + 1)
    }

    private val RAW_OFFSET = Regex("offset (\\d+)")
}

/** Immutable tree node used by the JSON viewer. */
sealed interface JsonNode {
    data class ObjectNode(val entries: List<Pair<String, JsonNode>>) : JsonNode
    data class ArrayNode(val items: List<JsonNode>) : JsonNode
    data class ValueNode(val primitive: JsonPrimitive) : JsonNode
    data object NullNode : JsonNode {
        val preview: String get() = "null"
    }
}

fun JsonNode.summary(): String = when (this) {
    is JsonNode.ObjectNode -> "{ ${entries.size} key${if (entries.size == 1) "" else "s"} }"
    is JsonNode.ArrayNode -> "[ ${items.size} item${if (items.size == 1) "" else "s"} ]"
    is JsonNode.ValueNode -> primitive.toString()
    is JsonNode.NullNode -> "null"
}

fun JsonPrimitive.asLiteral(): String = when {
    this is JsonNull -> "null"
    booleanOrNull != null -> booleanOrNull.toString()
    longOrNull != null -> longOrNull.toString()
    doubleOrNull != null -> doubleOrNull.toString()
    else -> content
}
