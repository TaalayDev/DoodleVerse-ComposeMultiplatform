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
    val thumb: ByteArray?,
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

@Entity(tableName = "drawing_paths")
data class DrawingPathEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(name = "layer_id")
    val layerId: Long,
    @ColumnInfo(name = "brush_id")
    val brushId: Int,
    val color: Int,
    @ColumnInfo(name = "stroke_width")
    val strokeWidth: Float,
    @ColumnInfo(name = "start_point_x")
    val startPointX: Float,
    @ColumnInfo(name = "start_point_y")
    val startPointY: Float,
    @ColumnInfo(name = "end_point_x")
    val endPointX: Float,
    @ColumnInfo(name = "end_point_y")
    val endPointY: Float,
    val randoms: String,
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