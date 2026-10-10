package com.devkit.app.core.text

import com.devkit.app.core.ToolResult
import kotlin.math.abs
import kotlin.math.roundToInt

/** HEX / RGB / HSL conversion with strict parsing and clear errors. */
object ColorTools {

    data class Rgb(val r: Int, val g: Int, val b: Int, val a: Int = 255)
    data class Hsl(val h: Float, val s: Float, val l: Float)

    fun parse(input: String): ToolResult<Rgb> {
        val s = input.trim()
        if (s.isEmpty()) return ToolResult.Failure("Enter a color value first.")
        return when {
            s.startsWith("#") -> parseHex(s)
            s.startsWith("rgb", ignoreCase = true) -> parseRgbFunction(s)
            s.startsWith("hsl", ignoreCase = true) -> parseHslFunction(s)
            s.matches(Regex("[0-9A-Fa-f]{3,8}")) -> parseHex("#$s")
            s.split(",").size == 3 -> parseRgbFunction("rgb($s)")
            else -> ToolResult.Failure("Unrecognized color. Use #RRGGBB, rgb(r,g,b) or hsl(h,s%,l%).")
        }
    }

    private fun parseHex(input: String): ToolResult<Rgb> {
        val hex = input.removePrefix("#")
        return when (hex.length) {
            3, 4 -> {
                val expanded = hex.map { "$it$it" }.joinToString("")
                parseHex("#$expanded")
            }
            6, 8 -> {
                val r = hex.substring(0, 2).toIntOrNull(16)
                val g = hex.substring(2, 4).toIntOrNull(16)
                val b = hex.substring(4, 6).toIntOrNull(16)
                val a = if (hex.length == 8) hex.substring(6, 8).toIntOrNull(16) else 255
                if (r == null || g == null || b == null || a == null) {
                    ToolResult.Failure("'$input' contains non-hex characters.")
                } else ToolResult.Success(Rgb(r, g, b, a))
            }
            else -> ToolResult.Failure("Hex colors must have 3, 4, 6 or 8 digits.")
        }
    }

    private fun parseRgbFunction(input: String): ToolResult<Rgb> {
        val inner = input.substringAfter('(').substringBefore(')')
        val parts = inner.split(',').map { it.trim() }
        if (parts.size !in 3..4) return ToolResult.Failure("rgb() expects r, g, b (and optional a).")
        val values = parts.map { it.toIntOrNull() ?: return ToolResult.Failure("'$it' is not a number.") }
        if (values.take(3).any { it !in 0..255 }) return ToolResult.Failure("RGB components must be 0..255.")
        val a = values.getOrNull(3) ?: 255
        if (a !in 0..255) return ToolResult.Failure("Alpha must be 0..255.")
        return ToolResult.Success(Rgb(values[0], values[1], values[2], a))
    }

    private fun parseHslFunction(input: String): ToolResult<Rgb> {
        val inner = input.substringAfter('(').substringBefore(')')
        val parts = inner.split(',').map { it.trim().removeSuffix("%") }
        if (parts.size != 3) return ToolResult.Failure("hsl() expects hue, saturation%, lightness%.")
        val h = parts[0].toFloatOrNull() ?: return ToolResult.Failure("'${parts[0]}' is not a number.")
        val s = parts[1].toFloatOrNull() ?: return ToolResult.Failure("'${parts[1]}' is not a number.")
        val l = parts[2].toFloatOrNull() ?: return ToolResult.Failure("'${parts[2]}' is not a number.")
        if (s !in 0f..100f || l !in 0f..100f) return ToolResult.Failure("Saturation and lightness must be 0..100.")
        return ToolResult.Success(hslToRgb(Hsl(h, s, l)))
    }


    fun rgbToHex(rgb: Rgb): String {
        val base = "#%02X%02X%02X".format(rgb.r, rgb.g, rgb.b)
        return if (rgb.a == 255) base else "$base%02X".format(rgb.a)
    }

    fun rgbToHsl(rgb: Rgb): Hsl {
        val r = rgb.r / 255f
        val g = rgb.g / 255f
        val b = rgb.b / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        if (abs(max - min) < 1e-6f) return Hsl(0f, 0f, l * 100f)
        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        val h = when (max) {
            r -> ((g - b) / d + if (g < b) 6f else 0f)
            g -> ((b - r) / d + 2f)
            else -> ((r - g) / d + 4f)
        } * 60f
        return Hsl(h, s * 100f, l * 100f)
    }

    fun hslToRgb(hsl: Hsl): Rgb {
        val h = ((hsl.h % 360f) + 360f) % 360f / 360f
        val s = (hsl.s / 100f).coerceIn(0f, 1f)
        val l = (hsl.l / 100f).coerceIn(0f, 1f)
        if (s == 0f) {
            val v = (l * 255).roundToInt()
            return Rgb(v, v, v)
        }
        val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        return Rgb(
            (hueToRgb(p, q, h + 1f / 3f) * 255).roundToInt().coerceIn(0, 255),
            (hueToRgb(p, q, h) * 255).roundToInt().coerceIn(0, 255),
            (hueToRgb(p, q, h - 1f / 3f) * 255).roundToInt().coerceIn(0, 255),
        )
    }

    private fun hueToRgb(p: Float, q: Float, t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }

    /** Relative luminance for contrast reporting (WCAG). */
    fun relativeLuminance(rgb: Rgb): Double {
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(rgb.r) + 0.7152 * channel(rgb.g) + 0.0722 * channel(rgb.b)
    }

    fun contrastRatio(a: Rgb, b: Rgb): Double {
        val l1 = relativeLuminance(a)
        val l2 = relativeLuminance(b)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }
}
