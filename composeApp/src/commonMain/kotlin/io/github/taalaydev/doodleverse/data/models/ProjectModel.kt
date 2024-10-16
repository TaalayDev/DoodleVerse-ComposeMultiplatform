package io.github.taalaydev.doodleverse.data.models

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap

data class ProjectModel(
    val id: Long,
    val name: String,
    val layers: List<LayerModel>,
    val cachedBitmap: ImageBitmap? = null,
    val createdAt: Long,
    val lastModified: Long,
    val aspectRatio: Size = Size(1f, 1f),
    val zoomLevel: Double = 1.0,
) {
    val aspectRatioValue: Float
        get() = aspectRatio.width / aspectRatio.height

    companion object {
        // TODO: Used for demo purposes, remove later
        var currentProject: ProjectModel? = null
    }
}

data class LayerModel(
    val id: Long,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val isBackground: Boolean = false,
    val opacity: Double = 1.0,
    val paths: List<DrawingPath> = emptyList(),
)
