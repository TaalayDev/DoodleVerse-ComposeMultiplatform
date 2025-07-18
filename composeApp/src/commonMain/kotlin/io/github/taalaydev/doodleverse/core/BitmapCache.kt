package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import io.github.taalaydev.doodleverse.data.models.LayerModel

class BitmapCache {
    private val cache = mutableMapOf<Long, ImageBitmap>()
    private val maxCacheSize = 50

    fun get(layerId: Long): ImageBitmap? = cache[layerId]

    fun put(layerId: Long, bitmap: ImageBitmap) {
        if (cache.size >= maxCacheSize) {
            val keysToRemove = cache.keys.take(cache.size - maxCacheSize + 1)
            keysToRemove.forEach { cache.remove(it) }
        }
        cache[layerId] = bitmap.copy()
    }

    fun remove(layerId: Long) {
        cache.remove(layerId)
    }

    fun clear() {
        cache.clear()
    }

    fun getAllBitmaps(): Map<Long, ImageBitmap> {
        return cache.mapValues { (_, bitmap) -> bitmap.copy() }
    }

    fun restoreFromSnapshot(bitmaps: Map<Long, ImageBitmap>) {
        clear()
        bitmaps.forEach { (layerId, bitmap) ->
            cache[layerId] = bitmap.copy()
        }
    }

    fun getCombinedBitmap(layers: List<LayerModel>, canvasSize: Size): ImageBitmap? {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return null

        val combinedBitmap = ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())
        val canvas = Canvas(combinedBitmap)

        layers.filter { it.isVisible && it.opacity > 0.0 }.forEach { layer ->
            get(layer.id)?.let { bitmap ->
                canvas.drawImage(bitmap, Offset.Zero, Paint().apply {
                    alpha = layer.opacity.toFloat()
                })
            }
        }

        return combinedBitmap
    }
}