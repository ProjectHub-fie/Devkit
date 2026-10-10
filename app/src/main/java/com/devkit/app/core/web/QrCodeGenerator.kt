package com.devkit.app.core.web

import android.graphics.Bitmap
import android.graphics.Color
import com.devkit.app.core.ToolResult
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeGenerator {

    const val MAX_LENGTH = 2000

    /** Produces the raw module matrix; separated from Android graphics so it is unit testable. */
    fun encode(text: String, size: Int = 512, errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M): ToolResult<BitMatrix> {
        if (text.isBlank()) return ToolResult.Failure("Enter text or a URL to encode.")
        if (text.length > MAX_LENGTH) {
            return ToolResult.Failure("Text is too long (${text.length} characters). The limit is $MAX_LENGTH.")
        }
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to errorCorrection,
                EncodeHintType.MARGIN to 2,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            )
            ToolResult.Success(QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints))
        } catch (e: Exception) {
            ToolResult.Failure("Could not generate QR code: ${e.message}")
        }
    }

    fun toBitmap(matrix: BitMatrix, foreground: Int = Color.BLACK, background: Int = Color.WHITE): Bitmap {
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (matrix[x, y]) foreground else background
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
