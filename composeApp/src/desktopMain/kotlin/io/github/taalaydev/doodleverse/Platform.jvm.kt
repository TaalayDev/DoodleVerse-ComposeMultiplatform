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
import io.github.taalaydev.doodleverse.purchase.InAppPurchaseManager
import io.github.taalaydev.doodleverse.purchase.MockInAppPurchaseManager
import io.github.taalaydev.doodleverse.shared.DesktopDataStorage
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Shader as SkShader
import org.jetbrains.skiko.toBufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.Desktop
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val isWeb: Boolean = false
    override val isDesktop: Boolean = true
    override val isAndroid: Boolean = false
    override val isIos: Boolean = false
    override val projectRepo: ProjectRepository = getRepository()
    override val dispatcherIO = kotlinx.coroutines.Dispatchers.IO

    override fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
        saveBitmap(bitmap, filename, format)
    }

    override fun launchUrl(url: String): Boolean {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
            return true
        }
        return false
    }
}

class DesktopAnalytics : Analytics() {
    override fun logEvent(name: String, params: Map<Any?, Any>?) {
        // Not implemented
    }
}

actual fun getAnalytics(): Analytics = DesktopAnalytics()

fun saveBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
    val buffer = bitmap.asSkiaBitmap().toBufferedImage()
    val imageFormat = format.extension

    getSaveFileUri(imageFormat) { uri ->
        if (uri == null) {
            return@getSaveFileUri
        }

        val file = File(uri)
        ImageIO.write(buffer, imageFormat, file)
    }
}

val ImageFormat.skiaFormat: org.jetbrains.skia.EncodedImageFormat
    get() = when (this) {
        ImageFormat.PNG -> org.jetbrains.skia.EncodedImageFormat.PNG
        ImageFormat.JPG -> org.jetbrains.skia.EncodedImageFormat.JPEG
    }

fun getSaveFileUri(extension: String, callback: (String?) -> Unit) {
    val fileChooser = JFileChooser()
    fileChooser.dialogTitle = "Save File"
    fileChooser.fileFilter = FileNameExtensionFilter("$extension files", extension)
    val userSelection = fileChooser.showSaveDialog(null)
    if (userSelection == JFileChooser.APPROVE_OPTION) {
        var file = fileChooser.selectedFile
        if (!file.absolutePath.endsWith(".$extension")) {
            file = File(file.absolutePath + ".$extension")
        }
        callback(file.absolutePath)
    }
    callback(null)
}

actual fun imageBitmapByteArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray {
    val image = bitmap.asSkiaBitmap()
    val encodedData = Image.makeFromBitmap(image).encodeToData(format.skiaFormat)
    return encodedData?.bytes ?: ByteArray(0)

//    val buffer = bitmap.asSkiaBitmap().toBufferedImage()
//    val imageFormat = format.extension
//
//    val byteArrayOutputStream = java.io.ByteArrayOutputStream()
//    ImageIO.write(buffer, imageFormat, byteArrayOutputStream)
//    return byteArrayOutputStream.toByteArray()
}

actual fun imageBitmapFromByteArray(bytes: ByteArray, width: Int, height: Int): ImageBitmap {
    try {
        val skiaImage = Image.makeFromEncoded(bytes)
        return skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to create ImageBitmap from pixels", e)
    }
}

actual fun getColorFromBitmap(bitmap: ImageBitmap, x: Int, y: Int): Int? {
    val skiaBitmap = bitmap.asSkiaBitmap()
    return skiaBitmap.getColor(x, y)
}

actual fun getPlatformType(): PlatformType = PlatformType.DESKTOP

actual fun createDataStorage(): DataStorage = DesktopDataStorage()

actual class PixelBuffer actual constructor(private val bitmap: ImageBitmap) {
    actual val width: Int = bitmap.width
    actual val height: Int = bitmap.height
    private val buf: IntArray = IntArray(width * height)

    init {
        // Portable read path
        val pm = bitmap.toPixelMap()
        var idx = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                buf[idx++] = pm[x, y].toArgb()
            }
        }
    }

    actual fun get(x: Int, y: Int): Int = buf[y * width + x]

    actual fun set(x: Int, y: Int, argb: Int) {
        buf[y * width + x] = argb
    }

    actual fun flushTo(bitmap: ImageBitmap) {
        // Pack IntArray -> little-endian bytes
        val bytes = ByteArray(width * height * 4)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(buf)

        val info = ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL)
        val data = Data.makeFromBytes(bytes)
        val skImage = Image.makeRaster(info, data, width * 4)

        val composeImg = skImage.toComposeImageBitmap()

        // Draw composeImg onto destination bitmap
        val canvas = Canvas(bitmap)
        canvas.drawImageRect(
            image = composeImg,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(width, height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, height),
            paint = Paint() // reused if you prefer
        )
    }
}

actual fun createInAppPurchaseManager(): InAppPurchaseManager {
    return MockInAppPurchaseManager()
}

/**
 * Desktop (JVM) implementation of platform shader.
 * Skia on desktop supports shaders through SkSL, but accessing them from Compose
 * requires lower-level APIs. This implementation uses the procedural fallback.
 */
actual class PlatformShader private actual constructor() {
    private var effect: RuntimeEffect? = null
    private var layout: UniformLayout? = null
    private var exactSize: Int = 0

    private val buf: ByteBuffer
        get() = ByteBuffer
            .allocateDirect(exactSize)
            .order(ByteOrder.LITTLE_ENDIAN)
    private val uniforms = mutableMapOf<String, FloatArray>()

    constructor(effect: RuntimeEffect, layout: UniformLayout, exactSize: Int) : this() {
        this.effect = effect
        this.layout = layout
        this.exactSize = exactSize
    }

    private fun putFloat(off: Int, v: Float) { buf.putFloat(off, v) }
    private fun putFloats(off: Int, vararg vs: Float) {
        var o = off
        for (v in vs) { buf.putFloat(o, v); o += 4 }
    }

    actual fun setUniform(name: String, value: Float) {
        layout!!.offsetOf(name)?.takeIf { it + 4 <= exactSize }?.let { putFloat(it, value) }
    }

    actual fun setUniform(name: String, x: Float, y: Float) {
        layout!!.offsetOf(name)?.takeIf { it + 8 <= exactSize }?.let { putFloats(it, x, y) }
    }

    actual fun setUniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        layout!!.offsetOf(name)?.takeIf { it + 16 <= exactSize }?.let { putFloats(it, x, y, z, w) }
    }

    actual fun toShader(): Shader? {
        // Copy buffer -> byte[] for Data
        val out = ByteArray(exactSize)
        val dup = buf.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        dup.clear(); dup.get(out)

        val skShader: SkShader? = try {
            effect!!.makeShader(
                uniforms = Data.makeFromBytes(out),
                children = null,
                localMatrix = null
            )
        } catch (_: Throwable) {
            null
        }

        // If still null, it's almost certainly a size/layout mismatch or SkSL main signature mismatch.
        return skShader
    }

    companion object {
        fun fromSource(source: String): PlatformShader? = runCatching {
            val eff = RuntimeEffect.makeForShader(source)
            val parsed = parseUniformLayoutFromSkSL(source)
            val exact = tryUniformSizeReflect(eff) ?: parsed.size
            PlatformShader(eff, parsed, exact)
        }.getOrNull()
    }
}

data class UniformSlot(val offset: Int, val size: Int)

class UniformLayout(
    val slots: Map<String, UniformSlot>,
    val size: Int
) {
    fun offsetOf(name: String): Int? = slots[name]?.offset
}

/**
 * Try to get exact uniform size from Skiko if the method exists in your version.
 */
private fun tryUniformSizeReflect(effect: RuntimeEffect): Int? = runCatching {
    val m = effect::class.java.methods.firstOrNull { it.name == "getUniformSize" && it.parameterCount == 0 }
    (m?.invoke(effect) as? Int)
}.getOrNull()

/**
 * Parse lines like:
 *   uniform float u_time;
 *   uniform half2 u_pos;
 *   uniform float3 u_dir;
 *   uniform int4 u_mask;
 *   uniform bool u_flag;
 *
 * Packing rules (std140-ish):
 *  - scalar:  align 4,  size 4
 *  - vec2:    align 8,  size 8
 *  - vec3:    align 16, size 16 (padded)
 *  - vec4:    align 16, size 16
 * (Treat half/int/bool like float for packing — Skia uses 32-bit uniform storage.)
 */
private fun parseUniformLayoutFromSkSL(source: String): UniformLayout {
    val re = Regex("""(?m)^\s*uniform\s+(half|float|int|bool)([234])?\s+([A-Za-z_]\w*)\s*(?:\[\s*\d+\s*])?\s*;""")
    var cursor = 0
    val slots = mutableMapOf<String, UniformSlot>()

    for (m in re.findAll(source)) {
        val base = m.groupValues[1]       // half|float|int|bool
        val lanesStr = m.groupValues[2]   // 2|3|4 or ""
        val name = m.groupValues[3]

        val lanes = if (lanesStr.isEmpty()) 1 else lanesStr.toInt()

        val (align, size) = when (lanes) {
            1 -> 4 to 4
            2 -> 8 to 8
            3 -> 16 to 16 // vec3 is padded to 16
            else -> 16 to 16 // vec4
        }

        val aligned = ((cursor + align - 1) / align) * align
        slots[name] = UniformSlot(aligned, size)
        cursor = aligned + size
    }

    // Final size rounded up to 16 (Skia tends to round the UBO)
    val total = max(16, ((cursor + 15) / 16) * 16)
    return UniformLayout(slots, total)
}

/**
 * Desktop factory for creating platform-specific shaders
 */
actual object ShaderFactory {
    actual fun create(source: String): PlatformShader? = PlatformShader.fromSource(source)
    actual fun isSupported(): Boolean = true
}