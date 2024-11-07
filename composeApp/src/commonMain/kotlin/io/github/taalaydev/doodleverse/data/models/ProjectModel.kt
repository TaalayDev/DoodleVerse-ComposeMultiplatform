package io.github.taalaydev.doodleverse.data.models

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap

import io.github.taalaydev.doodleverse.shared.ProjectModel as ProjectEntity
import io.github.taalaydev.doodleverse.shared.LayerModel as LayerEntity
import io.github.taalaydev.doodleverse.shared.FrameModel as FrameEntity

data class ProjectModel(
    val id: Long,
    val name: String,
    val frames: List<FrameModel>,
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
    val paths: List<DrawingPath> = emptyList(),
)

fun ProjectModel.toEntity(): ProjectEntity {
    return ProjectEntity(
        id = id,
        name = name,
        thumbnail = "",
        created = createdAt,
        lastModified = lastModified,
        width = aspectRatio.width,
        height = aspectRatio.height,
        frames = emptyList(),
    )
}

fun ProjectEntity.toModel(): ProjectModel {
    return ProjectModel(
        id = id,
        name = name,
        frames = frames.map { it.toModel() },
        cachedBitmap = null,
        createdAt = created,
        lastModified = lastModified,
        aspectRatio = Size(width, height),
    )
}

fun FrameModel.toEntity(): FrameEntity {
    return FrameEntity(
        id = id,
        projectId = projectId,
        name = name,
        order = order,
        layers = layers.map { it.toEntity() },
    )
}

fun FrameEntity.toModel(): FrameModel {
    return FrameModel(
        id = id,
        projectId = projectId,
        name = name,
        order = order,
        layers = layers.map { it.toModel() },
    )
}

fun LayerModel.toEntity(): LayerEntity {
    return LayerEntity(
        id = id,
        frameId = frameId,
        name = name,
        isVisible = isVisible,
        isLocked = isLocked,
        isBackground = isBackground,
        opacity = opacity,
        cachedBitmap = "",
        order = 0,
        drawingPaths = emptyList(),
    )
}

fun LayerEntity.toModel(): LayerModel {
    return LayerModel(
        id = id,
        frameId = frameId,
        name = name,
        isVisible = isVisible,
        isLocked = isLocked,
        isBackground = isBackground,
        opacity = opacity,
        paths = emptyList(),
    )
}