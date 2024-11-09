package io.github.taalaydev.doodleverse

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
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
}

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