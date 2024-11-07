package io.github.taalaydev.doodleverse

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import io.github.taalaydev.doodleverse.database.getRepository
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGImageAlphaInfo
import platform.Foundation.NSData
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import platform.posix.memcpy

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val isWeb: Boolean = false
    override val isDesktop: Boolean = false
    override val isAndroid: Boolean = false
    override val isIos: Boolean = true
    override val projectRepo: ProjectRepository = getRepository()
}


@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
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
actual fun imageBitmapBytArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray {
    val uiImage = bitmap.toUIImage() ?: return byteArrayOf()
    val nsData: NSData? = when (format) {
        ImageFormat.PNG -> UIImagePNGRepresentation(uiImage)
        ImageFormat.JPG -> UIImageJPEGRepresentation(uiImage, 1.0)
    }
    return nsData?.bytes?.readBytes(nsData.length.toInt()) ?: byteArrayOf()
}
