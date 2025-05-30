package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.graphics.ImageBitmap
import io.github.taalaydev.doodleverse.data.models.LayerModel

interface DrawingOperations {
    suspend fun addLayer(layer: LayerModel): Long
    suspend fun deleteLayer(layer: LayerModel)
    suspend fun updateLayer(layer: LayerModel, bitmap: ImageBitmap?)
    suspend fun saveProject()
}