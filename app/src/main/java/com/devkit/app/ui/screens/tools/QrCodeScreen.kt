package com.devkit.app.ui.screens.tools

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.devkit.app.core.ToolResult
import com.devkit.app.core.web.QrCodeGenerator
import com.devkit.app.data.repo.DevKitRepository
import com.devkit.app.ui.components.CodeEditor
import com.devkit.app.ui.components.StatusBanner
import com.devkit.app.util.compute
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun QrCodeScreen(repository: DevKitRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("https://developer.android.com") }
    var bitmap by rememberSaveable { mutableStateOf<Bitmap?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    ToolScaffoldWrapper("QR Code Generator", "Preview, share or save the generated image", onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CodeEditor(value = text, onValueChange = { text = it }, label = "Text or URL", minLines = 3, maxLines = 5)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (text.isBlank()) {
                        error = "Enter some text before generating a QR code."
                        bitmap = null
                        return@Button
                    }
                    scope.launch {
                        val result = compute {
                            QrCodeGenerator.encode(text.trim())
                        }
                        when (result) {
                            is ToolResult.Success -> {
                                bitmap = result.value.let { matrix -> QrCodeGenerator.toBitmap(matrix) }
                                error = null
                            }
                            is ToolResult.Failure -> { bitmap = null; error = result.message }
                        }
                    }
                }) { Text("Generate") }
                OutlinedButton(
                    enabled = bitmap != null,
                    onClick = { bitmap?.let { shareBitmap(context, it, "devkit-qr.png") } },
                ) { Text("Share image") }
            }
            error?.let { StatusBanner(it, isError = true) }
            bitmap?.let { image ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                ) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = "Generated QR code",
                            modifier = Modifier.size(260.dp),
                        )
                    }
                }
            }
            Text(
                "Very long input may exceed QR capacity; in that case a clear error is shown instead of a broken image.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, fileName)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share QR code"))
}
