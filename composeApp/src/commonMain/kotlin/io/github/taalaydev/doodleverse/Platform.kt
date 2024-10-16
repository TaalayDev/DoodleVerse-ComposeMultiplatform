package io.github.taalaydev.doodleverse

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import kotlin.reflect.KClass

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

enum class ImageFormat {
    PNG, JPG
}

@Composable
expect fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat)

expect fun imageBitmapBytArray(bitmap: ImageBitmap, format: ImageFormat): ByteArray