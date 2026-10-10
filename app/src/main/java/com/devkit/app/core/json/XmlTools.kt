package com.devkit.app.core.json

import com.devkit.app.core.ToolResult
import java.io.StringReader
import java.io.StringWriter
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Node
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

/**
 * XML formatting/validation. External entity resolution and DTD loading are
 * disabled so hostile documents cannot read local files or expand entities.
 */
object XmlTools {

    private class CollectingErrorHandler : ErrorHandler {
        var fatal: String? = null
        override fun warning(e: SAXParseException) = Unit
        override fun error(e: SAXParseException) {
            if (fatal == null) fatal = format(e)
        }
        override fun fatalError(e: SAXParseException) {
            fatal = format(e)
        }
        private fun format(e: SAXParseException) = "Line ${e.lineNumber}, column ${e.columnNumber}: ${e.message}"
    }

    private fun secureFactory(): DocumentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        isXIncludeAware = false
        isExpandEntityReferences = false
        isNamespaceAware = true
    }

    fun format(input: String, indent: Int = 2): ToolResult<String> {
        if (input.isBlank()) return ToolResult.Failure("Input is empty. Paste some XML first.")
        val handler = CollectingErrorHandler()
        return try {
            val factory = secureFactory()
            val builder = factory.newDocumentBuilder().apply { setErrorHandler(handler) }
            val document = builder.parse(InputSource(StringReader(input)))
            handler.fatal?.let { return ToolResult.Failure("Invalid XML: $it") }

            val transformerFactory = TransformerFactory.newInstance().apply {
                runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
                runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
                runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "") }
            }
            val transformer = transformerFactory.newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty(OutputKeys.ENCODING, "UTF-8")
                setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent.coerceIn(1, 8).toString())
            }
            val writer = StringWriter()
            transformer.transform(DOMSource(document), StreamResult(writer))
            ToolResult.Success(writer.toString().trim())
        } catch (e: Exception) {
            val detail = handler.fatal ?: e.message ?: "parse error"
            ToolResult.Failure("Invalid XML: $detail")
        }
    }

    fun validate(input: String): ToolResult<Unit> = format(input).let {
        when (it) {
            is ToolResult.Success -> ToolResult.Success(Unit)
            is ToolResult.Failure -> it
        }
    }

    /** Minifies by stripping inter-element whitespace. */
    fun minify(input: String): ToolResult<String> {
        val parsed = format(input)
        return when (parsed) {
            is ToolResult.Failure -> parsed
            is ToolResult.Success -> ToolResult.Success(
                parsed.value.lineSequence()
                    .joinToString("") { it.trim() }
                    .replace(Regex(">\\s+<"), "><")
                    .trim()
            )
        }
    }

    /** Used by tests to make sure the parser never touches the network/filesystem. */
    internal fun secureParseForTest(input: String): Result<Node> = runCatching {
        // no systemId is set, so external resolution cannot be triggered
        secureFactory().newDocumentBuilder().parse(InputSource(StringReader(input)))
    }
}
