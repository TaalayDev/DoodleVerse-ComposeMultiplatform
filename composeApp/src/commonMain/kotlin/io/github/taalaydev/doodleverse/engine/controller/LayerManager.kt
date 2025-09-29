package io.github.taalaydev.doodleverse.engine.controller

import androidx.compose.ui.graphics.ImageBitmap
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.engine.BitmapCache
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LayerManager(
    private val bitmapCache: BitmapCache,
    private val onLayerChanged: (LayerModel, ImageBitmap?) -> Unit
) {

    fun addLayer(frame: FrameModel, name: String): FrameModel {
        val newLayer = LayerModel(
            id = generateLayerId(),
            frameId = frame.id,
            name = name,
        )

        return frame.copy(layers = frame.layers + newLayer)
    }

    fun deleteLayer(frame: FrameModel, layerIndex: Int): FrameModel {
        if (layerIndex < 0 || layerIndex >= frame.layers.size) return frame

        val layerToDelete = frame.layers[layerIndex]
        bitmapCache.remove(layerToDelete.id)

        val newLayers = frame.layers.toMutableList().apply { removeAt(layerIndex) }
        return frame.copy(layers = newLayers)
    }

    fun updateLayerVisibility(frame: FrameModel, layerIndex: Int, isVisible: Boolean): FrameModel {
        if (layerIndex < 0 || layerIndex >= frame.layers.size) return frame

        val newLayers = frame.layers.toMutableList()
        val updatedLayer = newLayers[layerIndex].copy(isVisible = isVisible)
        newLayers[layerIndex] = updatedLayer

        onLayerChanged(updatedLayer, bitmapCache.get(updatedLayer.id))

        return frame.copy(layers = newLayers)
    }

    fun updateLayerOpacity(frame: FrameModel, layerIndex: Int, opacity: Float): FrameModel {
        if (layerIndex < 0 || layerIndex >= frame.layers.size) return frame

        val newLayers = frame.layers.toMutableList()
        val updatedLayer = newLayers[layerIndex].copy(opacity = opacity.toDouble())
        newLayers[layerIndex] = updatedLayer

        onLayerChanged(updatedLayer, bitmapCache.get(updatedLayer.id))

        return frame.copy(layers = newLayers)
    }

    fun reorderLayers(frame: FrameModel, fromIndex: Int, toIndex: Int): FrameModel {
        if (fromIndex < 0 || fromIndex >= frame.layers.size ||
            toIndex < 0 || toIndex >= frame.layers.size) return frame

        val newLayers = frame.layers.toMutableList()
        val layer = newLayers.removeAt(fromIndex)
        newLayers.add(toIndex, layer)

        return frame.copy(layers = newLayers)
    }

    fun updateLayerBitmap(frame: FrameModel, layerIndex: Int, bitmap: ImageBitmap): FrameModel {
        if (layerIndex < 0 || layerIndex >= frame.layers.size) return frame

        val newLayers = frame.layers.toMutableList()
        val updatedLayer = newLayers[layerIndex]
        newLayers[layerIndex] = updatedLayer

        bitmapCache.put(updatedLayer.id, bitmap)
        onLayerChanged(updatedLayer, bitmap)

        return frame.copy(layers = newLayers)
    }

    @OptIn(ExperimentalTime::class)
    private fun generateLayerId(): Long = Clock.System.now().toEpochMilliseconds() + (0..1000).random()
}