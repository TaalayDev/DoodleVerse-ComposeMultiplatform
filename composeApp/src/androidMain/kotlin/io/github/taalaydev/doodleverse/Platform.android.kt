package io.github.taalaydev.doodleverse

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import io.github.taalaydev.doodleverse.purchase.AndroidInAppPurchaseManager
import io.github.taalaydev.doodleverse.purchase.InAppPurchaseManager
import io.github.taalaydev.doodleverse.shared.AndroidDataStorage
import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import androidx.core.net.toUri

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
    //Canvas().drawCircle()
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

actual fun createDataStorage(): DataStorage = AndroidDataStorage(DoodleVerseApp.instance)

actual class PixelBuffer actual constructor(private val bitmap: ImageBitmap) {
    private val abm = bitmap.asAndroidBitmap() // same backing
    actual val width: Int = abm.width
    actual val height: Int = abm.height

    // single working buffer
    private val buf: IntArray = IntArray(width * height)

    init {
        // Read once into RAM buffer
        abm.getPixels(buf, 0, width, 0, 0, width, height)
    }

    actual fun get(x: Int, y: Int): Int = buf[y * width + x]

    actual fun set(x: Int, y: Int, argb: Int) {
        buf[y * width + x] = argb
    }

    actual fun flushTo(bitmap: ImageBitmap) {
        // Writes back into the same underlying Android Bitmap
        abm.setPixels(buf, 0, width, 0, 0, width, height)
    }
}

// Factory implementation
actual fun createInAppPurchaseManager(): InAppPurchaseManager {
    return AndroidInAppPurchaseManager(DoodleVerseApp.instance)
}

fun createInAppPurchaseManager(context: Context): InAppPurchaseManager {
    return AndroidInAppPurchaseManager(context)
}

/**
 * Android implementation of platform shader using RuntimeShader (API 33+)
 */
actual class PlatformShader private actual constructor() {
    private var runtimeShader: RuntimeShader? = null
    constructor(runtimeShader: RuntimeShader?) : this() {
        this.runtimeShader = runtimeShader
    }

    actual fun setUniform(name: String, value: Float) {
    }

    actual fun setUniform(name: String, x: Float, y: Float) {
    }

    actual fun setUniform(
        name: String,
        x: Float,
        y: Float,
        z: Float,
        w: Float
    ) {
    }

    actual fun toShader(): Shader? {
        return null
    }
}

actual object ShaderFactory {
    actual fun create(source: String): PlatformShader? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val shader = RuntimeShader(source)
                PlatformShader(shader)
            } catch (e: Exception) {
                // Shader compilation failed, return null to use fallback
                println("Shader compilation failed: ${e.message}")
                null
            }
        } else {
            // RuntimeShader not available on this API level
            null
        }
    }

    actual fun isSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

}