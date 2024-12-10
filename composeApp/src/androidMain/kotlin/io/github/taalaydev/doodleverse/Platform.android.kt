package io.github.taalaydev.doodleverse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

fun Map<Any?, Any>.toBundle(): android.os.Bundle {
    val bundle = android.os.Bundle()
    for ((key, value) in this) {
        when (value) {
            is String -> bundle.putString(key.toString(), value)
            is Int -> bundle.putInt(key.toString(), value)
            is Long -> bundle.putLong(key.toString(), value)
            is Double -> bundle.putDouble(key.toString(), value)
            is Boolean -> bundle.putBoolean(key.toString(), value)
            is Array<*> -> bundle.putStringArray(key.toString(), value as Array<String>)
            is IntArray -> bundle.putIntArray(key.toString(), value)
            is LongArray -> bundle.putLongArray(key.toString(), value)
            is DoubleArray -> bundle.putDoubleArray(key.toString(), value)
            is BooleanArray -> bundle.putBooleanArray(key.toString(), value)
            is Map<*, *> -> bundle.putBundle(key.toString(), (value as Map<Any?, Any>).toBundle())
            else -> throw IllegalArgumentException("Unsupported value type: ${value.javaClass}")
        }
    }
    return bundle
}

class AndroidAnalytics : Analytics() {
    override fun logEvent(name: String, params: Map<Any?, Any>?) {
        Firebase.analytics.logEvent(name, params?.toBundle())
    }
}

actual fun getAnalytics(): Analytics = AndroidAnalytics()

fun saveImageBitmap(context: Context, bitmap: ImageBitmap, filename: String, format: ImageFormat) {
    val bitmap = bitmap.asAndroidBitmap()
    val compressFormat = when (format) {
        ImageFormat.PNG -> Bitmap.CompressFormat.PNG
        ImageFormat.JPG -> Bitmap.CompressFormat.JPEG
    }

    getSaveFileUri(context, format.name.lowercase()) { uri ->
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

private fun getSaveFileUri(context: Context, extension: String, callback: (String?) -> Unit) {
    val activity = context as? ComponentActivity

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

actual fun imageBitmapByteArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray {
    val bitmap = bitmap.asAndroidBitmap()
    val compressFormat = when (format) {
        ImageFormat.PNG -> Bitmap.CompressFormat.PNG
        ImageFormat.JPG -> Bitmap.CompressFormat.JPEG
    }

    val stream = ByteArrayOutputStream()
    bitmap.compress(compressFormat, 100, stream)
    return stream.toByteArray()
}

actual fun imageBitmapFromByteArray(bytes: ByteArray, width: Int, height: Int): ImageBitmap {
    return try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(bytes))
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to create ImageBitmap from bytes", e)
    }
}

actual fun getColorFromBitmap(bitmap: ImageBitmap, x: Int, y: Int): Int? {
    val skiaBitmap = bitmap.asAndroidBitmap()
    return skiaBitmap.getPixel(x, y)
}

actual fun getPlatformType(): PlatformType = PlatformType.ANDROID