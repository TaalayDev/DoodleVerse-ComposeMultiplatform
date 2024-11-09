package io.github.taalaydev.doodleverse.data.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.toArgb
import io.github.taalaydev.doodleverse.ImageFormat
import io.github.taalaydev.doodleverse.imageBitmapByteArray
import io.github.taalaydev.doodleverse.imageBitmapFromByteArray

import io.github.taalaydev.doodleverse.shared.ProjectModel as ProjectData
import io.github.taalaydev.doodleverse.shared.LayerModel as LayerData
import io.github.taalaydev.doodleverse.shared.FrameModel as FrameData
import io.github.taalaydev.doodleverse.shared.DrawingPathModel as DrawingPathData
import io.github.taalaydev.doodleverse.shared.PointModel as PointData

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

fun ProjectModel.toEntity(): ProjectData {
    return ProjectData(
        id = id,
        name = name,
        thumbnail = "",
        created = createdAt,
        lastModified = lastModified,
        width = aspectRatio.width,
        height = aspectRatio.height,
        frames = emptyList(),
        thumb = if (cachedBitmap != null) imageBitmapByteArray(cachedBitmap, ImageFormat.PNG) else null,
    )
}

fun ProjectData.toModel(): ProjectModel {
    return ProjectModel(
        id = id,
        name = name,
        frames = frames.map { it.toModel() },
        cachedBitmap = if (thumb != null) imageBitmapFromByteArray(
            thumb!!,
            width.toInt(),
            height.toInt()
        ) else null,
        createdAt = created,
        lastModified = lastModified,
        aspectRatio = Size(width, height),
    )
}

fun FrameModel.toEntity(): FrameData {
    return FrameData(
        id = id,
        projectId = projectId,
        name = name,
        order = order,
        layers = layers.map { it.toEntity() },
    )
}

fun FrameData.toModel(): FrameModel {
    return FrameModel(
        id = id,
        projectId = projectId,
        name = name,
        order = order,
        layers = layers.map { it.toModel() },
    )
}

fun LayerModel.toEntity(): LayerData {
    return LayerData(
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
        pixels = byteArrayOf(),
        width = 0,
        height = 0,
    )
}

fun LayerData.toModel(): LayerModel {
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

fun DrawingPathData.toModel(): DrawingPath {
    return DrawingPath(
        color = Color(color),
        size = strokeWidth,
        path = DrawingPath.pathFromOffsets(points.map { it.toModel() }),
        brush = BrushData.all().first { it.id == brushId },
        points = points.map { it.toModel() }.toMutableList(),
        startPoint = Offset(startPointX, startPointY),
        endPoint = Offset(endPointX, endPointY),
        randoms = randomsList().toMutableMap(),
    )
}

fun DrawingPath.toEntity(): DrawingPathData {
    return DrawingPathData(
        id = 0,
        layerId = 0,
        brushId = brush.id,
        color = color.toArgb(),
        strokeWidth = size,
        startPointX = startPoint.x,
        startPointY = startPoint.y,
        endPointX = endPoint.x,
        endPointY = endPoint.y,
        points = points.map { it.toEntity() },
        randoms = randomsString(),
    )
}

fun PointData.toModel(): PointModel {
    return PointModel(x, y)
}

fun PointModel.toEntity(): PointData {
    return PointData(0L,0, x, y)
}