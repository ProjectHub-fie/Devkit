package com.devkit.app.core.encoding

import com.devkit.app.core.ToolResult
import java.nio.charset.StandardCharsets
import java.util.Base64

/** Base64, URL, HTML entity coding, radix conversion and Unicode inspection. */
object EncodingTools {

    // ---------- Base64 ----------

    data class Base64Output(val standard: String, val urlSafe: String)

    fun base64Encode(input: String): ToolResult<Base64Output> = try {
        val bytes = input.toByteArray(StandardCharsets.UTF_8)
        ToolResult.Success(
            Base64Output(
                standard = Base64.getEncoder().encodeToString(bytes),
                urlSafe = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            )
        )
    } catch (e: Exception) {
        ToolResult.Failure("Could not encode text: ${e.message}")
    }

    fun base64Decode(input: String, urlSafe: Boolean = false): ToolResult<String> {
        val cleaned = input.trim().replace(Regex("\\s"), "")
        if (cleaned.isEmpty()) return ToolResult.Failure("Input is empty. Paste a Base64 value first.")
        if (!cleaned.matches(Regex("^[A-Za-z0-9+/_=-]+$"))) {
            return ToolResult.Failure("Input contains characters that are not valid Base64.")
        }
        return try {
            val normalized = cleaned.replace('-', '+').replace('_', '/')
            val padded = when (normalized.length % 4) {
                0 -> normalized
                2 -> normalized + "=="
                3 -> normalized + "="
                else -> return ToolResult.Failure(
                    "Invalid Base64 length (${normalized.length} characters)."
                )
            }
            val decoder = if (urlSafe) Base64.getUrlDecoder() else Base64.getDecoder()
            ToolResult.Success(String(decoder.decode(padded), StandardCharsets.UTF_8))
        } catch (e: IllegalArgumentException) {
            ToolResult.Failure("Invalid Base64 input. Check the padding and characters.")
        }
    }

    // ---------- URL ----------

    /**
     * Component encoding uses percent-encoding for everything outside the
     * unreserved set, which is what you want inside a query value or path
     * segment. It deliberately does not encode '/'.
     */
    fun urlEncodeComponent(input: String): String {
        val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
        val sb = StringBuilder()
        for (byte in input.toByteArray(StandardCharsets.UTF_8)) {
            val c = (byte.toInt() and 0xFF).toChar()
            if (c in unreserved) sb.append(c) else sb.append('%').append("%02X".format(byte.toInt() and 0xFF))
        }
        return sb.toString()
    }

    /** Full-URL encoding keeps RFC 3986 reserved characters so the structure stays readable. */
    fun urlEncodeFull(input: String): String {
        val safe = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~/?:#[]@!$&'()*+,;="
        return input.toByteArray(StandardCharsets.UTF_8).joinToString("") { b ->
            val i = b.toInt() and 0xFF
            val c = i.toChar()
            if (c in safe) c.toString() else "%" + "%02X".format(i)
        }
    }

    fun urlDecode(input: String): ToolResult<String> = try {
        ToolResult.Success(percentDecode(input))
    } catch (e: Exception) {
        ToolResult.Failure("Invalid percent-encoding: ${e.message}")
    }

    private fun percentDecode(input: String): String {
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c == '%') {
                if (i + 2 >= input.length) {
                    throw IllegalArgumentException("truncated percent escape at index $i")
                }
                val hex = input.substring(i + 1, i + 3)
                val value = hex.toIntOrNull(16)
                    ?: throw IllegalArgumentException("invalid percent escape '%$hex'")
                out.write(value)
                i += 3
            } else {
                out.write(c.toString().toByteArray(StandardCharsets.UTF_8))
                i++
            }
        }
        return String(out.toByteArray(), StandardCharsets.UTF_8)
    }

    // ---------- HTML entities ----------

    private val NAMED = mapOf(
        "&" to "&amp;", "<" to "&lt;", ">" to "&gt;", "\"" to "&quot;", "'" to "&#39;"
    )
    private val DECODE = NAMED.entries.associate { (k, v) -> v to k } +
        mapOf("&apos;" to "'", "&nbsp;" to "\u00A0")

    fun htmlEncode(input: String, encodeAll: Boolean = false): String = buildString {
        for (c in input) {
            val s = c.toString()
            if (NAMED.containsKey(s)) append(NAMED[s])
            else if (encodeAll && c.code > 127) append("&#${c.code};")
            else append(c)
        }
    }

    fun htmlDecode(input: String): ToolResult<String> = try {
        val decoded = Regex("&(#x?[0-9A-Fa-f]+|[A-Za-z]+);").replace(input) { match ->
            val entity = match.groupValues[1]
            when {
                entity.startsWith("#x") || entity.startsWith("#X") ->
                    Character.toChars(entity.substring(2).toInt(16)).concatToString()
                entity.startsWith("#") ->
                    Character.toChars(entity.substring(1).toInt()).concatToString()
                DECODE.containsKey("&$entity;") -> DECODE.getValue("&$entity;")
                else -> match.value
            }
        }
        ToolResult.Success(decoded)
    } catch (e: Exception) {
        ToolResult.Failure("Could not decode HTML entities: ${e.message}")
    }

    // ---------- Radix ----------

    data class RadixOutput(val hex: String, val decimal: String, val binary: String, val octal: String)

    fun fromDecimal(input: String): ToolResult<RadixOutput> {
        val trimmed = input.trim().replace("_", "")
        if (trimmed.isEmpty()) return ToolResult.Failure("Enter a number first.")
        val value = trimmed.toLongOrNull()
            ?: return ToolResult.Failure("'$trimmed' is not a valid decimal integer (64-bit range).")
        return ToolResult.Success(
            RadixOutput(
                hex = "0x" + value.toString(16).uppercase(),
                decimal = value.toString(10),
                binary = "0b" + value.toString(2),
                octal = "0o" + value.toString(8)
            )
        )
    }

    fun parseAny(input: String): ToolResult<RadixOutput> {
        val trimmed = input.trim().replace("_", "")
        if (trimmed.isEmpty()) return ToolResult.Failure("Enter a number first.")
        val (radix, body) = when {
            trimmed.startsWith("0x", true) -> 16 to trimmed.substring(2)
            trimmed.startsWith("0b", true) -> 2 to trimmed.substring(2)
            trimmed.startsWith("0o", true) -> 8 to trimmed.substring(2)
            else -> 10 to trimmed
        }
        val value = body.toLongOrNull(radix)
            ?: return ToolResult.Failure("'$trimmed' is not valid for base $radix.")
        return ToolResult.Success(
            RadixOutput(
                hex = "0x" + value.toString(16).uppercase(),
                decimal = value.toString(10),
                binary = "0b" + value.toString(2),
                octal = "0o" + value.toString(8)
            )
        )
    }

    // ---------- Unicode inspector ----------

    data class UnicodeInfo(
        val index: Int,
        val codePoint: Int,
        val name: String,
        val category: String,
        val utf8: String,
        val utf16: String,
    )

    fun inspect(input: String): List<UnicodeInfo> {
        val result = mutableListOf<UnicodeInfo>()
        var i = 0
        var index = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            val count = Character.charCount(cp)
            result.add(
                UnicodeInfo(
                    index = index++,
                    codePoint = cp,
                    name = unicodeName(cp),
                    category = typeName(Character.getType(cp)),
                    utf8 = utf8Bytes(cp),
                    utf16 = utf16Units(input, i, count)
                )
            )
            i += count
        }
        return result
    }

    private fun utf8Bytes(cp: Int): String =
        String(Character.toChars(cp)).toByteArray(StandardCharsets.UTF_8)
            .joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

    private fun utf16Units(input: String, start: Int, count: Int): String {
        val sb = StringBuilder()
        for (k in 0 until count) {
            if (k > 0) sb.append(' ')
            sb.append("U+%04X".format(input[start + k].code))
        }
        return sb.toString()
    }

    private fun unicodeName(cp: Int): String = runCatching {
        when {
            cp == 0x20 -> "SPACE"
            cp < 0x20 -> "CONTROL"
            else -> {
                val block = Character.UnicodeBlock.of(cp)?.toString() ?: "Unassigned"
                val printable = if (Character.isDefined(cp)) String(Character.toChars(cp)) else "?"
                "$printable ($block)"
            }
        }
    }.getOrDefault("?")

    private fun typeName(type: Int): String = when (type) {
        Character.UPPERCASE_LETTER.toInt() -> "Uppercase letter"
        Character.LOWERCASE_LETTER.toInt() -> "Lowercase letter"
        Character.DECIMAL_DIGIT_NUMBER.toInt() -> "Digit"
        Character.SPACE_SEPARATOR.toInt() -> "Space"
        Character.CONTROL.toInt() -> "Control"
        Character.OTHER_SYMBOL.toInt() -> "Symbol"
        Character.MATH_SYMBOL.toInt() -> "Math symbol"
        Character.OTHER_PUNCTUATION.toInt(),
        Character.CONNECTOR_PUNCTUATION.toInt(),
        Character.DASH_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(),
        Character.END_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
        Character.FINAL_QUOTE_PUNCTUATION.toInt() -> "Punctuation"
        else -> "Character"
    }
}
