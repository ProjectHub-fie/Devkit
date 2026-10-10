package com.devkit.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/** Clipboard helper exposing a short-lived "copied" flag for UI feedback. */
class ClipboardHelper(private val context: Context) {
    var copiedSignal by mutableStateOf(false)
        private set

    fun copy(text: String) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("Dev Kit", text))
        copiedSignal = true
    }

    fun resetSignal() {
        copiedSignal = false
    }
}

@Composable
fun rememberClipboard(): ClipboardHelper {
    val context = LocalContext.current
    return remember(context) { ClipboardHelper(context) }
}

fun shareText(context: Context, text: String, subject: String = "Shared from Dev Kit") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

/** Writes text to the app cache and shares it as a file (used to export collections). */
fun shareTextFile(context: Context, fileName: String, text: String, mimeType: String = "application/json") {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeText(text)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export"))
}
