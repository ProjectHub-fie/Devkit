package com.devkit.app.core.json

import com.devkit.app.core.ToolResult
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.representer.Representer

/**
 * JSON <-> YAML conversion built on SnakeYAML's SafeConstructor. The safe
 * constructor is used so untrusted input can never instantiate arbitrary
 * classes during parsing.
 */
object YamlTools {

    private fun safeYaml(): Yaml {
        val loaderOptions = LoaderOptions().apply {
            isAllowDuplicateKeys = false
            codePointLimit = 5 * 1024 * 1024
        }
        val dumperOptions = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
        }
        val representer = Representer(dumperOptions)
        return Yaml(SafeConstructor(loaderOptions), representer, dumperOptions)
    }

    fun jsonToYaml(input: String): ToolResult<String> {
        val parsed = JsonTools.validate(input)
        return when (parsed) {
            is ToolResult.Failure -> parsed
            is ToolResult.Success -> try {
                val plain = toPlain(parsed.value)
                ToolResult.Success(safeYaml().dump(plain))
            } catch (e: Exception) {
                ToolResult.Failure("Could not convert JSON to YAML: ${e.message}")
            }
        }
    }

    fun yamlToJson(input: String, indent: Int = 2): ToolResult<String> {
        if (input.isBlank()) return ToolResult.Failure("Input is empty. Paste some YAML first.")
        return try {
            val loaded = safeYaml().load<Any?>(input)
            val json = toJsonString(loaded, indent)
            ToolResult.Success(json)
        } catch (e: Exception) {
            ToolResult.Failure("Invalid YAML: ${e.message?.lineSequence()?.firstOrNull() ?: "parse error"}")
        }
    }

    private fun toPlain(element: kotlinx.serialization.json.JsonElement): Any? = when (element) {
        is kotlinx.serialization.json.JsonObject -> element.entries.associate { (k, v) -> k to toPlain(v) }
        is kotlinx.serialization.json.JsonArray -> element.map { toPlain(it) }
        is kotlinx.serialization.json.JsonNull -> null
        is kotlinx.serialization.json.JsonPrimitive -> when {
            element.isString -> element.content
            element.content == "true" || element.content == "false" -> element.content.toBoolean()
            element.content.toLongOrNull() != null -> element.content.toLong()
            element.content.toDoubleOrNull() != null -> element.content.toDouble()
            else -> element.content
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toJsonString(value: Any?, indent: Int): String {
        val e = fromPlain(value)
        return JsonTools.prettyPrint(
            kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(), e
            ),
            indent
        ).getOrNull() ?: "null"
    }

    private fun fromPlain(value: Any?): kotlinx.serialization.json.JsonElement = when (value) {
        null -> kotlinx.serialization.json.JsonNull
        is Map<*, *> -> kotlinx.serialization.json.JsonObject(
            value.entries.associate { (k, v) -> k.toString() to fromPlain(v) }
        )
        is List<*> -> kotlinx.serialization.json.JsonArray(value.map { fromPlain(it) })
        is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
        is Number -> kotlinx.serialization.json.JsonPrimitive(value)
        else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
    }
}
