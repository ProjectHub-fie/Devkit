package com.devkit.app.core.text

import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Markdown rendering via commonmark-java. Raw HTML in the source is escaped
 * (escapeHtml) so embedded scripts are never emitted as live markup, and the
 * caller renders the result as selectable text rather than a WebView.
 */
object MarkdownTools {

    private val parser: Parser = Parser.builder().build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder()
        .escapeHtml(true)
        .softbreak("<br/>\n")
        .build()

    fun toHtml(markdown: String): String {
        if (markdown.isBlank()) return ""
        val document: Node = parser.parse(markdown)
        return renderer.render(document)
    }

    /** Plain-text preview that keeps headings and lists readable without HTML. */
    fun toPlainText(markdown: String): String {
        if (markdown.isBlank()) return ""
        return markdown.lineSequence().joinToString("\n") { line ->
            when {
                line.startsWith("###### ") -> line.removePrefix("###### ")
                line.startsWith("##### ") -> line.removePrefix("##### ")
                line.startsWith("#### ") -> line.removePrefix("#### ")
                line.startsWith("### ") -> line.removePrefix("### ")
                line.startsWith("## ") -> line.removePrefix("## ")
                line.startsWith("# ") -> line.removePrefix("# ")
                else -> line
            }
        }
    }
}
