package io.github.taalaydev.doodleverse.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.taalaydev.doodleverse.database.entities.LayerEntity
import io.github.taalaydev.doodleverse.database.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LayerDao {
    @Query("SELECT * FROM layers WHERE frame_id = :frameId")
    fun getAllLayers(frameId: Long): Flow<List<LayerEntity>>

    @Query("SELECT * FROM layers WHERE id = :id")
    suspend fun getLayerById(id: Long): LayerEntity

    @Insert
    suspend fun insertLayer(layer: LayerEntity): Long

    @Insert
    suspend fun insertLayers(layers: List<LayerEntity>): List<Long>

    @Update
    suspend fun updateLayer(layer: LayerEntity)

    @Query("DELETE FROM layers WHERE id = :id")
    suspend fun deleteLayerById(id: Long)

    @Query("DELETE FROM layers")
    suspend fun deleteAllLayers()

    @Query("UPDATE layers SET `order` = :order WHERE id = :id")
    suspend fun updateLayerOrder(id: Long, order: Int)
}