package io.github.taalaydev.doodleverse.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.taalaydev.doodleverse.database.entities.FrameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FrameDao {
    @Query("SELECT * FROM frames WHERE project_id = :projectId")
    suspend fun getAllFrames(projectId: Long): List<FrameEntity>

    @Query("SELECT * FROM frames WHERE id = :id")
    suspend fun getFrameById(id: Long): FrameEntity

    @Insert
    suspend fun insertFrame(frame: FrameEntity): Long

    @Insert
    suspend fun insertFrames(frames: List<FrameEntity>): List<Long>

    @Update
    suspend fun updateFrame(frame: FrameEntity)

    @Query("DELETE FROM frames WHERE id = :id")
    suspend fun deleteFrameById(id: Long)

    @Query("DELETE FROM frames")
    suspend fun deleteAllFrames()

    @Query("UPDATE frames SET `order` = :order WHERE id = :id")
    suspend fun updateFrameOrder(id: Long, order: Int)
}