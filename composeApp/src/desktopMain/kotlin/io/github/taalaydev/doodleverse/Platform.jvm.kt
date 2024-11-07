package io.github.taalaydev.doodleverse

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import io.github.taalaydev.doodleverse.database.getRepository
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import org.jetbrains.skiko.toBufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val isWeb: Boolean = false
    override val isDesktop: Boolean = true
    override val isAndroid: Boolean = false
    override val isIos: Boolean = false
    override val projectRepo: ProjectRepository = getRepository()
}


@Composable
actual fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
    val buffer = bitmap.asSkiaBitmap().toBufferedImage()
    val imageFormat = when (format) {
        ImageFormat.PNG -> "png"
        ImageFormat.JPG -> "jpg"
    }

    getSaveFileUri(imageFormat) { uri ->
        if (uri == null) {
            return@getSaveFileUri
        }

        val file = File(uri)
        ImageIO.write(buffer, imageFormat, file)
    }
}

@Composable
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

actual fun imageBitmapBytArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray {
    val buffer = bitmap.asSkiaBitmap().toBufferedImage()
    val imageFormat = when (format) {
        ImageFormat.PNG -> "png"
        ImageFormat.JPG -> "jpg"
    }

    val byteArrayOutputStream = java.io.ByteArrayOutputStream()
    ImageIO.write(buffer, imageFormat, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}