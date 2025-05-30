package io.github.taalaydev.doodleverse

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.taalaydev.doodleverse.database.getRepository
import io.github.taalaydev.doodleverse.shared.DesktopDataStorage
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import kotlinx.coroutines.IO
import org.jetbrains.skia.Image
import org.jetbrains.skiko.toBufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.Desktop
import java.net.URI

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