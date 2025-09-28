package io.github.taalaydev.doodleverse

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
import cocoapods.FirebaseAnalytics.FIRAnalytics
import io.github.taalaydev.doodleverse.purchase.IOSInAppPurchaseManager
import io.github.taalaydev.doodleverse.purchase.InAppPurchaseManager
import io.github.taalaydev.doodleverse.purchase.MockInAppPurchaseManager
import io.github.taalaydev.doodleverse.shared.IOSDataStorage
import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Data
import org.jetbrains.skia.ImageInfo
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol

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

class IOSAnalytics: Analytics() {
    @OptIn(ExperimentalForeignApi::class)
    override fun logEvent(name: String, params: Map<Any?, Any>?) {
        FIRAnalytics.logEventWithName(name, parameters = params)
    }

}

actual fun getAnalytics(): Analytics = IOSAnalytics()

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

actual fun createDataStorage(): DataStorage = IOSDataStorage()

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
        val bytes = ByteArray(width * height * 4)
        // Compose/Native doesn't have java.nio; do a manual pack:
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

// Factory implementation
actual fun createInAppPurchaseManager(): InAppPurchaseManager {
    return IOSInAppPurchaseManager()
    // return MockInAppPurchaseManager()
}

/**
 * iOS implementation of platform shader using Metal shaders
 * Note: This is a simplified implementation - full Metal integration would require
 * more setup with MTLDevice, MTLLibrary, etc.
 */
actual class PlatformShader  actual constructor() {
    private var metalDevice: MTLDeviceProtocol? = null
    private var functionName: String? = null

    private val uniforms = mutableMapOf<String, FloatArray>()

    constructor(metalDevice: MTLDeviceProtocol?, functionName: String?) : this() {
        this.metalDevice = metalDevice
        this.functionName = functionName
    }

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
        // Metal shader integration would require wrapping in a custom Shader
        // For now, return null to use procedural fallback
        // A full implementation would create a Metal shader pipeline
        return null
    }
}

/**
 * iOS factory for creating platform-specific shaders
 */
actual object ShaderFactory {

    actual fun create(source: String): PlatformShader? {
        // Check if Metal is available
        val device = MTLCreateSystemDefaultDevice()
        return if (device != null) {
            // In a full implementation, we would:
            // 1. Compile the Metal shader source
            // 2. Create a MTLLibrary from the compiled shader
            // 3. Get the function from the library
            // 4. Create a pipeline state
            // For now, return a placeholder that will use fallback
            PlatformShader(device, null)
        } else {
            null
        }
    }

    actual fun isSupported(): Boolean {
        // Check if Metal is available on this device
        return MTLCreateSystemDefaultDevice() != null
    }
}