package io.github.taalaydev.doodleverse.shared

data class ProjectModel(
    val id: Long,
    val name: String,
    val thumbnail: String,
    val created: Long,
    val lastModified: Long,
    val animationStates: List<AnimationStateModel>,
    val width: Float,
    val height: Float,
    val thumb: ByteArray?,
)

data class AnimationStateModel(
    val id: Long,
    val name: String,
    val duration: Long,
    val frames: List<FrameModel>,
    val projectId: Long,
)

data class FrameModel(
    val id: Long,
    val animationId: Long,
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
    val pixels: ByteArray,
    val width: Int,
    val height: Int,
) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

data class DrawingPathModel(
    val id: Long,
    val layerId: Long,
    val brushId: Int,
    val color: Int,
    val strokeWidth: Float,
    val startPointX: Float,
    val startPointY: Float,
    val endPointX: Float,
    val endPointY: Float,
    val points: List<PointModel>,
    val randoms: String
) {
    fun randomsList(): Map<String, Float> {
        return mutableMapOf()
    }
}

data class PointModel(
    val id: Long,
    val drawingPathId: Long,
    val x: Float,
    val y: Float,
)