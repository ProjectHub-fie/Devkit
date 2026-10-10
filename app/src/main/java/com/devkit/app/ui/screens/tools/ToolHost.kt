package com.devkit.app.ui.screens.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.devkit.app.core.ToolCatalog
import com.devkit.app.data.prefs.UserSettings
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.EmptyState
import com.devkit.app.ui.components.ToolScaffold
import kotlinx.coroutines.launch

/**
 * Routes a tool id to its screen and records usage for the "recently used"
 * list. If an id has no screen, an explicit "not implemented" state is shown
 * rather than a blank page — the catalog and this mapping are the only place
 * tools become visible to users.
 */
@Composable
fun ToolHost(
    toolId: String,
    repository: DevKitRepository,
    settings: UserSettings,
    onBack: () -> Unit,
    onOpenApiTester: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(toolId) {
        if (ToolCatalog.byId(toolId) != null) {
            scope.launch { repository.recordToolUsage(toolId) }
        }
    }

    when (toolId) {
        // JSON & data
        "json_formatter" -> JsonFormatterScreen(repository, onBack, onOpenApiTester)
        "json_viewer" -> JsonViewerScreen(repository, onBack)
        "json_yaml" -> JsonYamlScreen(repository, onBack)
        "xml_formatter" -> XmlFormatterScreen(repository, onBack)
        "sql_formatter" -> SqlFormatterScreen(repository, onBack)

        // Encoding
        "base64" -> Base64Screen(repository, onBack)
        "url_codec" -> UrlCodecScreen(repository, onBack)
        "html_entities" -> HtmlEntitiesScreen(repository, onBack)
        "unicode_inspector" -> UnicodeInspectorScreen(repository, onBack)
        "radix_converter" -> RadixConverterScreen(repository, onBack)
        "timestamp" -> TimestampScreen(repository, onBack)

        // Generators
        "uuid" -> UuidScreen(repository, onBack)
        "random_string" -> RandomStringScreen(repository, onBack)
        "hash" -> HashGeneratorScreen(repository, onBack)
        "qr_code" -> QrCodeScreen(repository, onBack)

        // Text
        "word_count" -> WordCountScreen(repository, onBack)
        "case_converter" -> CaseConverterScreen(repository, onBack)
        "text_diff" -> TextDiffScreen(repository, onBack)
        "regex_tester" -> RegexTesterScreen(repository, onBack)
        "markdown_preview" -> MarkdownPreviewScreen(repository, onBack)
        "color_converter" -> ColorConverterScreen(repository, onBack)
        "lorem_ipsum" -> LoremIpsumScreen(repository, onBack)
        "cron_helper" -> CronHelperScreen(repository, onBack)
        "date_converter" -> DateConverterScreen(repository, onBack)
        "snippets" -> SnippetsScreen(repository, onBack)

        // Security
        "jwt_decoder" -> JwtDecoderScreen(repository, onBack)
        "hash_inspect" -> HashInspectorScreen(repository, onBack)

        // Web & API
        "api_tester" -> onOpenApiTester()
        "url_parser" -> UrlParserScreen(repository, onBack)
        "user_agent" -> HeaderBuilderScreen(repository, onBack)

        // Converters
        "csv_json" -> CsvJsonScreen(repository, onBack)
        "string_escape" -> StringEscapeScreen(repository, onBack)

        else -> ToolScaffold(title = "Tool unavailable", onBack = onBack) {
            EmptyState(
                icon = Icons.Filled.Warning,
                title = "This tool is not available",
                message = "The tool id \"$toolId\" has no implementation in this build.",
            )
        }
    }
}
