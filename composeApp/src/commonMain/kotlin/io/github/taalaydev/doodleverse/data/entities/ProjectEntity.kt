package io.github.taalaydev.doodleverse.data.entities

import kotlinx.datetime.Clock

data class ProjectEntity(
    val id: Long,
    val name: String,
    val thumbnail: String,
    val layers: List<LayerEntity>,
    val created: Long = Clock.System.now().toEpochMilliseconds(),
    val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
)

data class LayerEntity(
    val id: Long,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val isBackground: Boolean = false,
    val opacity: Double = 1.0,
    val cachedBitmap: String,
    val paths: List<DrawingPathEntity> = emptyList(),
)

data class DrawingPathEntity(
    val id: Long,
    val color: Int,
    val strokeWidth: Float,
    val points: List<PointEntity>,
)

data class PointEntity(
    val id: String,
    val x: Float,
    val y: Float,
)