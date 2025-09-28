package io.github.taalaydev.doodleverse.engine.controller

import androidx.compose.ui.graphics.ImageBitmap
import io.github.taalaydev.doodleverse.data.models.LayerModel

interface DrawOperations {
    suspend fun addLayer(layer: LayerModel): Long
    suspend fun deleteLayer(layer: LayerModel)
    suspend fun updateLayer(layer: LayerModel, bitmap: ImageBitmap?)
    suspend fun saveProject()
}