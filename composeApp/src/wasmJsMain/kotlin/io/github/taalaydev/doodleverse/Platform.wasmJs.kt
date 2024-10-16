package io.github.taalaydev.doodleverse

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
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
}

actual fun getPlatform(): Platform = WasmPlatform()

@Composable
actual fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
    val skiaBitmap = bitmap.asSkiaBitmap()
    val skiaImage = Image.makeFromBitmap(skiaBitmap)

    val encodedData = when (format) {
        ImageFormat.PNG -> skiaImage.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
        ImageFormat.JPG -> skiaImage.encodeToData(org.jetbrains.skia.EncodedImageFormat.JPEG)
    } ?: return

    val bytes = encodedData.bytes
    val uint8Array = Uint8Array(bytes.size)
}

actual fun imageBitmapBytArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray {
    val skiaBitmap = bitmap.asSkiaBitmap()
    val skiaImage = Image.makeFromBitmap(skiaBitmap)

    val encodedData = when (format) {
        ImageFormat.PNG -> skiaImage.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)
        ImageFormat.JPG -> skiaImage.encodeToData(org.jetbrains.skia.EncodedImageFormat.JPEG)
    } ?: return byteArrayOf()

    return encodedData.bytes
}