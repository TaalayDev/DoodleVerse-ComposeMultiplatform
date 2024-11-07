package io.github.taalaydev.doodleverse.shared

import kotlinx.coroutines.flow.Flow

abstract class ProjectRepository {
    abstract fun getAllProjects(): Flow<List<ProjectModel>>
    abstract suspend fun getProjectById(id: Long): ProjectModel
    abstract suspend fun insertProject(project: ProjectModel): Long
    abstract suspend fun updateProject(project: ProjectModel)
    abstract suspend fun insertProjects(projects: List<ProjectModel>)
    abstract suspend fun deleteProjectById(id: Long)
    abstract suspend fun deleteAllProjects()

    abstract fun getAllFrames(projectId: Long): Flow<List<FrameModel>>
    abstract suspend fun getFrameById(id: Long): FrameModel
    abstract suspend fun insertFrame(frame: FrameModel): Long
    abstract suspend fun updateFrame(frame: FrameModel)
    abstract suspend fun insertFrames(frames: List<FrameModel>)
    abstract suspend fun deleteFrameById(id: Long)
    abstract suspend fun deleteAllFrames()
    abstract suspend fun updateFrameOrder(id: Long, order: Int)

    abstract fun getAllLayers(frameId: Long): Flow<List<LayerModel>>
    abstract suspend fun getLayerById(id: Long): LayerModel
    abstract suspend fun insertLayer(layer: LayerModel): Long
    abstract suspend fun updateLayer(layer: LayerModel)
    abstract suspend fun insertLayers(layers: List<LayerModel>): List<Long>
    abstract suspend fun deleteLayerById(id: Long)
    abstract suspend fun deleteAllLayers()
}