package io.github.taalaydev.doodleverse.database.entities

import androidx.room.ColumnInfo
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
    @ColumnInfo(name = "last_modified")
    val lastModified: Long = Clock.System.now().toEpochMilliseconds(),
    val width: Float,
    val height: Float,
)

@Entity(tableName = "frames")
data class FrameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(name = "project_id")
    val projectId: Long,
    val name: String,
    val order: Int = 0,
)

@Entity(tableName = "layers")
data class LayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(name = "frame_id")
    val frameId: Long,
    val name: String,
    @ColumnInfo(name = "is_visible")
    val isVisible: Boolean = true,
    @ColumnInfo(name = "is_locked")
    val isLocked: Boolean = false,
    @ColumnInfo(name = "is_background")
    val isBackground: Boolean = false,
    val opacity: Double = 1.0,
    @ColumnInfo(name = "cached_bitmap")
    val cachedBitmap: String,
    val order: Int = 0,
)

@Entity(tableName = "drawing_paths")
data class DrawingPathEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(name = "layer_id")
    val layerId: Long,
    val color: Int,
    @ColumnInfo(name = "stroke_width")
    val strokeWidth: Float,
)

@Entity(tableName = "points")
data class PointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(name = "drawing_path_id")
    val drawingPathId: Long,
    val x: Float,
    val y: Float,
)