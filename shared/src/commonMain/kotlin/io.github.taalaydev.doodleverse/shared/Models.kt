package io.github.taalaydev.doodleverse.shared

data class ProjectModel(
    val id: Long,
    val name: String,
    val thumbnail: String,
    val created: Long,
    val lastModified: Long,
    val frames: List<FrameModel>,
    val width: Float,
    val height: Float,
)

data class FrameModel(
    val id: Long,
    val projectId: Long,
    val name: String,
    val order: Int = 0,
    val layers: List<LayerModel>,
)

data class LayerModel(
    val id: Long,
    val frameId: Long,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val isBackground: Boolean = false,
    val opacity: Double = 1.0,
    val cachedBitmap: String,
    val order: Int = 0,
    val drawingPaths: List<DrawingPathModel>,
)

data class DrawingPathModel(
    val id: Long,
    val layerId: Long,
    val color: Int,
    val strokeWidth: Float,
    val points: List<PointModel>,
)

data class PointModel(
    val id: String,
    val drawingPathId: Long,
    val x: Float,
    val y: Float,
)