package io.github.taalaydev.doodleverse.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.taalaydev.doodleverse.database.entities.PointEntity
import io.github.taalaydev.doodleverse.database.entities.ProjectEntity

@Dao
interface PointDao {
    @Insert
    suspend fun insertPoint(point: PointEntity): Long

    @Update
    suspend fun updatePoint(point: PointEntity)

    @Insert
    suspend fun insertPoints(points: List<PointEntity>): List<Long>

    @Query("SELECT * FROM points WHERE id = :id")
    suspend fun getPointById(id: Long): PointEntity

    @Query("SELECT * FROM points WHERE drawing_path_id = :pathId")
    suspend fun getPointsByPathId(pathId: Long): List<PointEntity>

    @Query("SELECT * FROM points")
    suspend fun getAllPoints(): List<PointEntity>

    @Query("DELETE FROM points WHERE id = :id")
    suspend fun deletePointById(id: Long)

    @Query("DELETE FROM points")
    suspend fun deleteAllPoints()
}