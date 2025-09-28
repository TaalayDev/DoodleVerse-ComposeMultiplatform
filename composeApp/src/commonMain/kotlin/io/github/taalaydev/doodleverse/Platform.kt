package io.github.taalaydev.doodleverse

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shader
import io.github.taalaydev.doodleverse.purchase.InAppPurchaseManager
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import kotlinx.coroutines.CoroutineDispatcher

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

    fun dataStorage(): DataStorage = DataStorageFactory.create()
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

/**
 * Internal factory responsible for creating the appropriate platform implementation
 */
object DataStorageFactory {
    private var instance: DataStorage? = null

    fun create(): DataStorage {
        if (instance == null) {
            instance = createDataStorage()
        }
        return instance!!
    }
}

expect fun createDataStorage(): DataStorage

/**
 * Expect/Actual fast pixel access for flood fill & sampling without toPixelMap()
 * Implement actuals with Skia on each platform for speed.
 */
expect class PixelBuffer(bitmap: ImageBitmap) {
    val width: Int
    val height: Int
    fun get(x: Int, y: Int): Int
    fun set(x: Int, y: Int, argb: Int)
    fun flushTo(bitmap: ImageBitmap)
}

expect fun createInAppPurchaseManager(): InAppPurchaseManager

/**
 * Platform-specific shader implementation
 */
expect class PlatformShader private constructor() {
    fun setUniform(name: String, value: Float)
    fun setUniform(name: String, x: Float, y: Float)
    fun setUniform(name: String, x: Float, y: Float, z: Float, w: Float)
    fun toShader(): Shader?
}

/**
 * Factory for creating platform-specific shaders
 */
expect object ShaderFactory {
    fun create(source: String): PlatformShader?
    fun isSupported(): Boolean
}