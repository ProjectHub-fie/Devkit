package com.devkit.app.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(val id: String, val label: String) {
    JSON_DATA("json_data", "JSON & Data"),
    ENCODING("encoding", "Encoders & Decoders"),
    GENERATORS("generators", "Generators"),
    TEXT("text", "Text Tools"),
    WEB_API("web_api", "Web & API"),
    SECURITY("security", "Security & Inspection"),
    CONVERTERS("converters", "Converters"),
}

data class ToolDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: ToolCategory,
    val route: String,
    val icon: ImageVector,
    val keywords: List<String> = emptyList(),
)

/**
 * Single source of truth for the catalog. The Home, Tools, search and Saved
 * screens all read from here, and every entry has a registered route in
 * [com.devkit.app.ui.navigation.DevKitNavHost], so nothing is shown that is not
 * implemented.
 */
object ToolCatalog {

    val tools: List<ToolDefinition> = listOf(
        // ---- JSON & Data ----
        ToolDefinition(
            "json_formatter", "JSON Formatter", "Pretty-print, minify and validate JSON",
            ToolCategory.JSON_DATA, "tool/json_formatter", Icons.Filled.DataObject,
            listOf("json", "format", "pretty", "minify", "validate", "beautify")
        ),
        ToolDefinition(
            "json_viewer", "JSON Viewer", "Explore nested objects and arrays",
            ToolCategory.JSON_DATA, "tool/json_viewer", Icons.Filled.AccountTree,
            listOf("json", "view", "tree", "explore")
        ),
        ToolDefinition(
            "json_yaml", "JSON to YAML", "Convert between JSON and YAML",
            ToolCategory.JSON_DATA, "tool/json_yaml", Icons.Filled.CompareArrows,
            listOf("json", "yaml", "yml", "convert")
        ),
        ToolDefinition(
            "xml_formatter", "XML Formatter", "Format and validate XML safely",
            ToolCategory.JSON_DATA, "tool/xml_formatter", Icons.Filled.Code,
            listOf("xml", "format", "validate", "html")
        ),
        ToolDefinition(
            "sql_formatter", "SQL Formatter", "Format common SQL statements",
            ToolCategory.JSON_DATA, "tool/sql_formatter", Icons.Filled.Storage,
            listOf("sql", "query", "format", "database")
        ),

        // ---- Encoders & Decoders ----
        ToolDefinition(
            "base64", "Base64 Encoder", "Encode and decode Base64 text",
            ToolCategory.ENCODING, "tool/base64", Icons.Filled.SwapHoriz,
            listOf("base64", "encode", "decode", "b64")
        ),
        ToolDefinition(
            "url_codec", "URL Encoder", "Percent-encode and decode URLs",
            ToolCategory.ENCODING, "tool/url_codec", Icons.Filled.Link,
            listOf("url", "uri", "encode", "decode", "percent")
        ),
        ToolDefinition(
            "html_entities", "HTML Entities", "Encode and decode HTML entities",
            ToolCategory.ENCODING, "tool/html_entities", Icons.Filled.Tag,
            listOf("html", "entity", "escape", "unescape")
        ),
        ToolDefinition(
            "unicode_inspector", "Unicode Inspector", "Inspect code points and UTF bytes",
            ToolCategory.ENCODING, "tool/unicode_inspector", Icons.Filled.Language,
            listOf("unicode", "codepoint", "utf8", "emoji", "inspect")
        ),
        ToolDefinition(
            "radix_converter", "Number Base Converter", "Hex, decimal, binary and octal",
            ToolCategory.ENCODING, "tool/radix_converter", Icons.Filled.Calculate,
            listOf("hex", "binary", "octal", "decimal", "convert", "base", "radix")
        ),
        ToolDefinition(
            "timestamp", "Timestamp Converter", "Unix time to dates across time zones",
            ToolCategory.ENCODING, "tool/timestamp", Icons.Filled.Schedule,
            listOf("timestamp", "unix", "epoch", "date", "time", "timezone")
        ),

        // ---- Generators ----
        ToolDefinition(
            "uuid", "UUID Generator", "Generate random and time-ordered UUIDs",
            ToolCategory.GENERATORS, "tool/uuid", Icons.Filled.Fingerprint,
            listOf("uuid", "guid", "v4", "v7", "id")
        ),
        ToolDefinition(
            "random_string", "Random String", "Generate random strings and passwords",
            ToolCategory.GENERATORS, "tool/random_string", Icons.Filled.Casino,
            listOf("random", "password", "string", "generate")
        ),
        ToolDefinition(
            "hash", "Hash Generator", "MD5, SHA-1, SHA-256 and HMAC digests",
            ToolCategory.GENERATORS, "tool/hash", Icons.Filled.Tag,
            listOf("hash", "sha", "md5", "checksum", "digest", "hmac")
        ),
        ToolDefinition(
            "qr_code", "QR Code Generator", "Create QR codes from text or URLs",
            ToolCategory.GENERATORS, "tool/qr_code", Icons.Filled.QrCode2,
            listOf("qr", "barcode", "generate", "share")
        ),

        // ---- Text Tools ----
        ToolDefinition(
            "word_count", "Word Counter", "Count characters, words, lines and paragraphs",
            ToolCategory.TEXT, "tool/word_count", Icons.Filled.TextFields,
            listOf("word", "character", "count", "lines")
        ),
        ToolDefinition(
            "case_converter", "Case Converter", "camelCase, snake_case and more",
            ToolCategory.TEXT, "tool/case_converter", Icons.Filled.TextRotationNone,
            listOf("case", "camel", "snake", "kebab", "pascal", "uppercase", "lowercase")
        ),
        ToolDefinition(
            "text_diff", "Text Diff", "Compare two texts line by line",
            ToolCategory.TEXT, "tool/text_diff", Icons.Filled.Difference,
            listOf("diff", "compare", "text", "changes")
        ),
        ToolDefinition(
            "regex_tester", "Regex Tester", "Test regular expressions safely",
            ToolCategory.TEXT, "tool/regex_tester", Icons.Filled.Search,
            listOf("regex", "regexp", "pattern", "match", "replace")
        ),
        ToolDefinition(
            "markdown_preview", "Markdown Preview", "Render Markdown without executing HTML",
            ToolCategory.TEXT, "tool/markdown_preview", Icons.Filled.Article,
            listOf("markdown", "md", "preview", "render")
        ),
        ToolDefinition(
            "color_converter", "Color Converter", "HEX, RGB and HSL conversion",
            ToolCategory.TEXT, "tool/color_converter", Icons.Filled.Palette,
            listOf("color", "hex", "rgb", "hsl", "contrast")
        ),
        ToolDefinition(
            "lorem_ipsum", "Lorem Ipsum", "Generate placeholder text",
            ToolCategory.TEXT, "tool/lorem_ipsum", Icons.Filled.FormatQuote,
            listOf("lorem", "ipsum", "placeholder", "dummy")
        ),
        ToolDefinition(
            "cron_helper", "Cron Helper", "Validate and explain cron expressions",
            ToolCategory.TEXT, "tool/cron_helper", Icons.Filled.Timer,
            listOf("cron", "schedule", "expression", "validate")
        ),
        ToolDefinition(
            "date_converter", "Date Converter", "Convert between dates and timestamps",
            ToolCategory.TEXT, "tool/date_converter", Icons.Filled.Event,
            listOf("date", "time", "convert", "iso", "format")
        ),
        ToolDefinition(
            "snippets", "Snippets", "Store and search reusable code snippets",
            ToolCategory.TEXT, "tool/snippets", Icons.Filled.CodeOff,
            listOf("snippet", "code", "save", "library")
        ),

        // ---- Security & Inspection ----
        ToolDefinition(
            "jwt_decoder", "JWT Decoder", "Inspect JWT header and payload",
            ToolCategory.SECURITY, "tool/jwt_decoder", Icons.Filled.Key,
            listOf("jwt", "token", "decode", "claims", "jsonwebtoken")
        ),
        ToolDefinition(
            "hash_inspect", "Hash Inspector", "Compare two hashes and identify the algorithm",
            ToolCategory.SECURITY, "tool/hash_inspect", Icons.Filled.Security,
            listOf("hash", "compare", "identify", "md5", "sha")
        ),

        // ---- Web & API ----
        ToolDefinition(
            "api_tester", "API Tester", "Send real HTTP requests and inspect responses",
            ToolCategory.WEB_API, "api", Icons.Filled.Cloud,
            listOf("api", "http", "rest", "request", "response", "tester", "postman")
        ),
        ToolDefinition(
            "url_parser", "URL Parser", "Break a URL into its components",
            ToolCategory.WEB_API, "tool/url_parser", Icons.Filled.Link,
            listOf("url", "parse", "query", "host", "path")
        ),
        ToolDefinition(
            "user_agent", "User-Agent Parser", "Read browser and device hints",
            ToolCategory.WEB_API, "tool/header_builder", Icons.Filled.Http,
            listOf("user-agent", "header", "request", "http")
        ),

        // ---- Converters ----
        ToolDefinition(
            "csv_json", "CSV to JSON", "Convert CSV rows into JSON",
            ToolCategory.CONVERTERS, "tool/csv_json", Icons.Filled.TableChart,
            listOf("csv", "json", "convert", "table")
        ),
        ToolDefinition(
            "string_escape", "String Escaper", "Escape and unescape strings for many languages",
            ToolCategory.CONVERTERS, "tool/string_escape", Icons.Filled.EscalatorWarning,
            listOf("escape", "unescape", "quotes", "string", "json", "java")
        ),
    )

    fun byId(id: String): ToolDefinition? = tools.firstOrNull { it.id == id }

    fun byRoute(route: String): ToolDefinition? = tools.firstOrNull { it.route == route }

    fun byCategory(category: ToolCategory): List<ToolDefinition> = tools.filter { it.category == category }

    fun search(query: String): List<ToolDefinition> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return tools.filter { tool ->
            tool.title.lowercase().contains(q) ||
                tool.description.lowercase().contains(q) ||
                tool.category.label.lowercase().contains(q) ||
                tool.keywords.any { it.contains(q) }
        }
    }
}
