package io.github.taalaydev.doodleverse

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.reflect.KClass

interface Platform {
    val name: String
    val isWeb: Boolean
    val isDesktop: Boolean
    val isAndroid: Boolean
    val isIos: Boolean
    val dispatcherIO: CoroutineDispatcher

    val projectRepo: ProjectRepository
    fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {}
}

enum class ImageFormat {
    PNG, JPG;

    val extension: String
        get() = when (this) {
            PNG -> "png"
            JPG -> "jpg"
        }
}

expect fun imageBitmapByteArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray

expect fun imageBitmapFromByteArray(bytes: ByteArray, width: Int, height: Int): ImageBitmap

expect fun getColorFromBitmap(bitmap: ImageBitmap, x: Int, y: Int): Int?