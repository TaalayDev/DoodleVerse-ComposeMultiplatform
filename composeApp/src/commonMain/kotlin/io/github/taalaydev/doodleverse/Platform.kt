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
    fun launchUrl(url: String): Boolean
}

abstract class Analytics {
    abstract fun logEvent(name: String, params: Map<Any?, Any>?)
}

expect fun getAnalytics(): Analytics

enum class PlatformType {
    WEB, DESKTOP, ANDROID, IOS;

    val isWeb: Boolean
        get() = this == WEB
    val isDesktop: Boolean
        get() = this == DESKTOP
    val isAndroid: Boolean
        get() = this == ANDROID
    val isIos: Boolean
        get() = this == IOS
}

expect fun getPlatformType(): PlatformType

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