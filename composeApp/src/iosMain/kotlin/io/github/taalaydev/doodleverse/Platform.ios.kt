package io.github.taalaydev.doodleverse

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.taalaydev.doodleverse.database.getRepository
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlinx.coroutines.IO
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGImageAlphaInfo
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.Foundation.NSProcessInfo
import platform.Foundation.isMacCatalystApp
import platform.Foundation.isiOSAppOnMac
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val isWeb: Boolean = false
    override val isDesktop: Boolean = NSProcessInfo.processInfo.isiOSAppOnMac()
    override val isAndroid: Boolean = false
    override val isIos: Boolean = !NSProcessInfo.processInfo.isiOSAppOnMac()
    override val projectRepo: ProjectRepository = getRepository()
    override val dispatcherIO = kotlinx.coroutines.Dispatchers.IO
    override fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
        saveBitmap(bitmap, filename, format)
    }
    override fun launchUrl(url: String): Boolean {
        val nsUrl = NSURL(string = url)
        return UIApplication.sharedApplication.openURL(nsUrl)
    }
}


@OptIn(ExperimentalForeignApi::class)
fun saveBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
    val image = bitmap.toUIImage() ?: return

    UIImageWriteToSavedPhotosAlbum(image, null, null, null)
}

@OptIn(ExperimentalForeignApi::class)
fun ImageBitmap.toUIImage(): UIImage? {
    val width = this.width
    val height = this.height
    val buffer = IntArray(width * height)

    this.readPixels(buffer)

    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val context = CGBitmapContextCreate(
        data = buffer.refTo(0),
        width = width.toULong(),
        height = height.toULong(),
        bitsPerComponent = 8u,
        bytesPerRow = (4 * width).toULong(),
        space = colorSpace,
        bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
    )

    val cgImage = CGBitmapContextCreateImage(context)
    return cgImage?.let { UIImage.imageWithCGImage(it) }
}

@OptIn(ExperimentalForeignApi::class)
actual fun imageBitmapByteArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray {
    val image = bitmap.asSkiaBitmap()
    val encodedData = Image.makeFromBitmap(image).encodeToData(format.skiaFormat)
    return encodedData?.bytes ?: ByteArray(0)
}

val ImageFormat.skiaFormat: org.jetbrains.skia.EncodedImageFormat
    get() = when (this) {
        ImageFormat.PNG -> org.jetbrains.skia.EncodedImageFormat.PNG
        ImageFormat.JPG -> org.jetbrains.skia.EncodedImageFormat.JPEG
    }

actual fun imageBitmapFromByteArray(bytes: ByteArray, width: Int, height: Int): ImageBitmap {
    try {
        val skiaImage = Image.makeFromEncoded(bytes)
        return skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to create ImageBitmap from bytes", e)
    }
}

actual fun getColorFromBitmap(bitmap: ImageBitmap, x: Int, y: Int): Int? {
    val skiaBitmap = bitmap.asSkiaBitmap()
    return skiaBitmap.getColor(x, y)
}

actual fun getPlatformType(): PlatformType = PlatformType.IOS
