package io.github.taalaydev.doodleverse.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.taalaydev.doodleverse.database.entities.DrawingPathEntity
import io.github.taalaydev.doodleverse.database.entities.ProjectEntity

@Dao
interface DrawingPathDao {
    @Insert
    suspend fun insertDrawingPath(drawingPath: DrawingPathEntity): Long

    @Insert
    suspend fun insertDrawingPaths(drawingPaths: List<DrawingPathEntity>): List<Long>

    @Update
    suspend fun updateDrawingPath(drawingPath: DrawingPathEntity)

    @Query("SELECT * FROM drawing_paths WHERE id = :id")
    suspend fun getDrawingPathById(id: Long): DrawingPathEntity

    @Query("SELECT * FROM drawing_paths WHERE layer_id = :layerId")
    suspend fun getDrawingPathsByLayerId(layerId: Long): List<DrawingPathEntity>

    @Query("SELECT * FROM drawing_paths")
    suspend fun getAllDrawingPaths(): List<DrawingPathEntity>

    @Query("DELETE FROM drawing_paths WHERE id = :id")
    suspend fun deleteDrawingPathById(id: Long)

    @Query("DELETE FROM drawing_paths")
    suspend fun deleteAllDrawingPaths()
}