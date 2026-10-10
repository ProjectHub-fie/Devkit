package com.devkit.app.core.web

import com.devkit.app.core.ToolResult
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * JWT inspection. This decodes and displays the token parts; it never verifies
 * a signature and the UI must say so. Payloads are treated as untrusted data.
 */
object JwtTools {

    data class Decoded(
        val header: String,
        val payload: String,
        val signaturePresent: Boolean,
        val algorithm: String?,
        val expired: Boolean?,
        val expiresAt: Long?,
        val issuedAt: Long?,
    )

    fun decode(token: String): ToolResult<Decoded> {
        val trimmed = token.trim().removePrefix("Bearer ").trim()
        if (trimmed.isEmpty()) return ToolResult.Failure("Paste a JWT first.")
        val parts = trimmed.split('.')
        if (parts.size < 2) return ToolResult.Failure("A JWT has at least two dot-separated parts.")
        return try {
            val headerBytes = decodePart(parts[0])
            val payloadBytes = decodePart(parts[1])
            val headerText = String(headerBytes, Charsets.UTF_8)
            val payloadText = String(payloadBytes, Charsets.UTF_8)

            val headerJson = Json.parseToJsonElement(headerText) as? JsonObject
            val payloadJson = Json.parseToJsonElement(payloadText) as? JsonObject

            val alg = headerJson?.get("alg")?.toString()?.trim('"')
            val exp = payloadJson?.get("exp")?.toString()?.trim('"')?.toLongOrNull()
            val iat = payloadJson?.get("iat")?.toString()?.trim('"')?.toLongOrNull()
            val nowSeconds = System.currentTimeMillis() / 1000
            ToolResult.Success(
                Decoded(
                    header = pretty(headerText),
                    payload = pretty(payloadText),
                    signaturePresent = parts.size >= 3 && parts[2].isNotEmpty(),
                    algorithm = alg,
                    expired = exp?.let { it < nowSeconds },
                    expiresAt = exp,
                    issuedAt = iat,
                )
            )
        } catch (e: IllegalArgumentException) {
            ToolResult.Failure("The token parts are not valid Base64URL. ${e.message ?: ""}".trim())
        } catch (e: Exception) {
            ToolResult.Failure("Could not decode token: ${e.message}")
        }
    }

    private fun decodePart(part: String): ByteArray {
        val normalized = part.replace('-', '+').replace('_', '/')
        val padded = when (normalized.length % 4) {
            0 -> normalized
            2 -> normalized + "=="
            3 -> normalized + "="
            else -> throw IllegalArgumentException("invalid segment length")
        }
        return Base64.getDecoder().decode(padded)
    }

    private fun pretty(text: String): String {
        return try {
            val element = Json.parseToJsonElement(text)
            val obj = element as? JsonObject ?: return text
            Json { prettyPrint = true; prettyPrintIndent = "  " }
                .encodeToString(JsonObject.serializer(), obj)
        } catch (e: Exception) {
            text
        }
    }
}
