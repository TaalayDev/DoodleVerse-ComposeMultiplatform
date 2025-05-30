package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import io.github.taalaydev.doodleverse.data.models.LayerModel

interface DrawProvider {
    val selectionState: SelectionState
    fun undo()
    fun redo()
    fun updateCurrentTool(tool: Tool)
    fun setColor(color: Color)
    fun startSelection(offset: Offset)
    fun updateSelection(offset: Offset)
    fun updateSelection(state: SelectionState)
    fun endSelection()
    fun applySelection()
    fun startTransform(transform: SelectionTransform, point: Offset)
    fun updateSelectionTransform(pan: Offset)
    fun updateSelectionTransform(centroid: Offset, pan: Offset, zoom: Float, rotation: Float)
    fun startMove(offset: Offset)
    fun updateMove(offset: Offset)
    fun endMove()
    fun floodFill(x: Int, y: Int)

    fun addLayer(layer: LayerModel)
    fun deleteLayer(layerId: Long)
    fun updateLayer(layerId: Long, updater: (LayerModel) -> LayerModel)
    suspend fun updateProject()
}