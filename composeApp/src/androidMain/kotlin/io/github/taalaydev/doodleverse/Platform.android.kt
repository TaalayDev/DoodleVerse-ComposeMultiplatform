package io.github.taalaydev.doodleverse

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.coroutines.resume

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

@Composable
actual fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
    val bitmap = bitmap.asAndroidBitmap()
    val compressFormat = when (format) {
        ImageFormat.PNG -> Bitmap.CompressFormat.PNG
        ImageFormat.JPG -> Bitmap.CompressFormat.JPEG
    }

    val context = LocalContext.current
    getSaveFileUri(format.name.lowercase()) { uri ->
        if (uri == null) {
            return@getSaveFileUri
        }

        val file = File(context.filesDir, filename)
        val stream = FileOutputStream(file)
        bitmap.compress(compressFormat, 100, stream)
        stream.close()

        val contentUri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
        context.sendBroadcast(intent)
    }
}

@Composable
fun getSaveFileUri(extension: String, callback: (String?) -> Unit) {
    val activity = LocalContext.current as? ComponentActivity

    if (activity == null) {
        callback(null)
        return
    }

    val mimeType = when (extension) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "json" -> "application/json"
        else -> "*/*"
    }

    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeType
        putExtra(Intent.EXTRA_TITLE, "myDrawing.$extension")
    }

    val launcher = activity.activityResultRegistry.register(
        "saveFile",
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            callback(uri?.toString())
        } else {
            callback(null)
        }
    }

    launcher.launch(intent)
}

actual fun imageBitmapBytArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray {
    val bitmap = bitmap.asAndroidBitmap()
    val compressFormat = when (format) {
        ImageFormat.PNG -> Bitmap.CompressFormat.PNG
        ImageFormat.JPG -> Bitmap.CompressFormat.JPEG
    }

    val stream = ByteArrayOutputStream()
    bitmap.compress(compressFormat, 100, stream)
    return stream.toByteArray()
}