package io.github.taalaydev.doodleverse

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.purchase.InAppPurchaseManager
import io.github.taalaydev.doodleverse.purchase.MockInAppPurchaseManager
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import io.github.taalaydev.doodleverse.shared.WebDataStorage
import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.khronos.webgl.Uint8Array
import org.w3c.dom.*
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val isWeb: Boolean = true
    override val isDesktop: Boolean = false
    override val isAndroid: Boolean = false
    override val isIos: Boolean = false
    override val projectRepo: ProjectRepository = DemoProjectRepo()
    override val dispatcherIO = kotlinx.coroutines.Dispatchers.Default

    override fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
        saveBitmap(bitmap, filename, format)
    }
    override fun launchUrl(url: String): Boolean {
        window.open(url, "_blank")
        return true
    }
}

class WebAnalytics: Analytics() {
    override fun logEvent(name: String, params: Map<Any?, Any>?) {
        // Not implemented
    }
}

actual fun getAnalytics(): Analytics = WebAnalytics()

fun saveBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
    val skiaBitmap = bitmap.asSkiaBitmap()
    val skiaImage = Image.makeFromBitmap(skiaBitmap)

    val encodedData = when (format) {
        ImageFormat.PNG -> skiaImage.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
        ImageFormat.JPG -> skiaImage.encodeToData(org.jetbrains.skia.EncodedImageFormat.JPEG)
    } ?: return

    val bytes = encodedData.bytes
    val uint8Array = Uint8Array(bytes.size)


}

actual fun imageBitmapByteArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray {
    val skiaBitmap = bitmap.asSkiaBitmap()
    val skiaImage = Image.makeFromBitmap(skiaBitmap)

    val encodedData = when (format) {
        ImageFormat.PNG -> skiaImage.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
        ImageFormat.JPG -> skiaImage.encodeToData(org.jetbrains.skia.EncodedImageFormat.JPEG)
    } ?: return byteArrayOf()

    return encodedData.bytes
}

actual fun imageBitmapFromByteArray(bytes: ByteArray, width: Int, height: Int): ImageBitmap {
    return try {
        // Use Skia's Image.makeFromEncoded to decode the bytes into a Skia Image
        val skiaImage = Image.makeFromEncoded(bytes)

        // Convert the Skia Image to a Compose ImageBitmap
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to create ImageBitmap from bytes", e)
    }
}

actual fun getColorFromBitmap(bitmap: ImageBitmap, x: Int, y: Int): Int? {
    val skiaBitmap = bitmap.asSkiaBitmap()
    return skiaBitmap.getColor(x, y)
}

actual fun getPlatformType(): PlatformType = PlatformType.WEB

actual fun createDataStorage(): DataStorage = WebDataStorage()

actual class PixelBuffer actual constructor(private val bitmap: ImageBitmap) {
    actual val width: Int = bitmap.width
    actual val height: Int = bitmap.height
    private val buf: IntArray = IntArray(width * height)

    init {
        val pm = bitmap.toPixelMap()
        var i = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                buf[i++] = pm[x, y].toArgb()
            }
        }
    }

    actual fun get(x: Int, y: Int): Int = buf[y * width + x]
    actual fun set(x: Int, y: Int, argb: Int) { buf[y * width + x] = argb }

    actual fun flushTo(bitmap: ImageBitmap) {
        // pack to LE bytes (manual to avoid java.nio)
        val bytes = ByteArray(width * height * 4)
        var bi = 0
        for (v in buf) {
            bytes[bi++] = (v and 0xFF).toByte()
            bytes[bi++] = ((v ushr 8) and 0xFF).toByte()
            bytes[bi++] = ((v ushr 16) and 0xFF).toByte()
            bytes[bi++] = ((v ushr 24) and 0xFF).toByte()
        }

        val info = ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL)
        val data = Data.makeFromBytes(bytes)
        val skImage = Image.makeRaster(info, data, width * 4)
        val composeImg = skImage.toComposeImageBitmap()

        val canvas = Canvas(bitmap)
        canvas.drawImageRect(
            image = composeImg,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(width, height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, height),
            paint = Paint()
        )
    }
}

actual fun createInAppPurchaseManager(): InAppPurchaseManager {
    return MockInAppPurchaseManager()
}

/**
 * Web (JavaScript) implementation of platform shader.
 * Could potentially use WebGL shaders, but for simplicity uses procedural fallback.
 */
actual class PlatformShader {
    private val uniforms = mutableMapOf<String, FloatArray>()

    actual fun setUniform(name: String, value: Float) {
        uniforms[name] = floatArrayOf(value)
    }

    actual fun setUniform(name: String, x: Float, y: Float) {
        uniforms[name] = floatArrayOf(x, y)
    }

    actual fun setUniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        uniforms[name] = floatArrayOf(x, y, z, w)
    }

    actual fun toShader(): Shader? {
        // On web, we could potentially use:
        // - WebGL shaders through Canvas 2D context
        // - WebGPU compute shaders (experimental)
        // - CSS Paint API with Houdini
        // For now, return null to use procedural fallback
        return null
    }
}

/**
 * Web factory for creating platform-specific shaders
 */
actual object ShaderFactory {

    actual fun createShader(source: String): PlatformShader? {
        // Web implementation could potentially use WebGL or WebGPU
        // For now, return null to use procedural fallback
        return null
    }

    actual fun isSupported(): Boolean {
        // Could check for WebGL/WebGPU support here
        // For now, report as unsupported to use procedural effects
        return false
    }
}