package io.github.taalaydev.doodleverse.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.taalaydev.doodleverse.database.entities.AnimationStateEntity

@Dao
interface AnimationStateDao {
    @Query("SELECT * FROM animation_states WHERE project_id = :projectId")
    suspend fun getAll(projectId: Long): List<AnimationStateEntity>

    @Query("SELECT * FROM animation_states WHERE id = :id")
    suspend fun getById(id: Long): AnimationStateEntity

    @Insert
    suspend fun insert(animation: AnimationStateEntity): Long

    @Insert
    suspend fun insert(animations: List<AnimationStateEntity>): List<Long>

    @Update
    suspend fun update(animation: AnimationStateEntity)

    @Query("DELETE FROM frames WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM animation_states")
    suspend fun deleteAll()
}