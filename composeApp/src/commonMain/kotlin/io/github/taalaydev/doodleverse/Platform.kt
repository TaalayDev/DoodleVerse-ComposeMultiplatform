package io.github.taalaydev.doodleverse

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlin.reflect.KClass

interface Platform {
    val name: String
    val isWeb: Boolean
    val isDesktop: Boolean
    val isAndroid: Boolean
    val isIos: Boolean

    val projectRepo: ProjectRepository
}

enum class ImageFormat {
    PNG, JPG
}

@Composable
expect fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat)

expect fun imageBitmapBytArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray