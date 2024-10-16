package io.github.taalaydev.doodleverse.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val thumbnail: String,
    val created: Long = Clock.System.now().toEpochMilliseconds(),
    val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
)

data class LayerEntity(
    val id: Long,
    val projectId: Long,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val isBackground: Boolean = false,
    val opacity: Double = 1.0,
    val cachedBitmap: String,
)

data class DrawingPathEntity(
    val id: Long,
    val layerId: Long,
    val color: Int,
    val strokeWidth: Float,
)

data class PointEntity(
    val id: String,
    val drawingPathId: Long,
    val x: Float,
    val y: Float,
)