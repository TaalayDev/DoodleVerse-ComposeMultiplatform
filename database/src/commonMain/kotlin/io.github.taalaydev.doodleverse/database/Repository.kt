package io.github.taalaydev.doodleverse.database

import io.github.taalaydev.doodleverse.data.database.dao.LayerDao
import io.github.taalaydev.doodleverse.data.database.dao.ProjectDao
import io.github.taalaydev.doodleverse.database.dao.FrameDao
import io.github.taalaydev.doodleverse.database.entities.LayerEntity
import io.github.taalaydev.doodleverse.database.entities.ProjectEntity
import io.github.taalaydev.doodleverse.database.entities.FrameEntity
import io.github.taalaydev.doodleverse.shared.FrameModel
import io.github.taalaydev.doodleverse.shared.LayerModel
import io.github.taalaydev.doodleverse.shared.ProjectModel
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ProjectRepositoryImpl(
    private val projectDao: ProjectDao,
    private val frameDao: FrameDao,
    private val layerDao: LayerDao,
): ProjectRepository() {
    // Projects

    override fun getAllProjects(): Flow<List<ProjectModel>> {
        return projectDao.getAllProjects().map { it.map { it.toModel() } }
    }

    override suspend fun getProjectById(id: Long): ProjectModel {
        return projectDao.getProjectById(id).toModel()
    }

    override suspend fun insertProject(project: ProjectModel): Long {
        return projectDao.insertProject(project.toEntity())
    }

    override suspend fun updateProject(project: ProjectModel) {
        projectDao.updateProject(project.toEntity())
    }

    override suspend fun insertProjects(projects: List<ProjectModel>) {
        projectDao.insertProjects(projects.map { it.toEntity() })
    }

    override suspend fun deleteProjectById(id: Long) {
        projectDao.deleteProjectById(id)
    }

    override suspend fun deleteAllProjects() {
        projectDao.deleteAllProjects()
    }

    // Frames

    override fun getAllFrames(projectId: Long): Flow<List<FrameModel>> {
        return frameDao.getAllFrames(projectId).map { it.map { it.toModel() } }
    }

    override suspend fun getFrameById(id: Long): FrameModel {
        return frameDao.getFrameById(id).toModel()
    }

    override suspend fun insertFrame(frame: FrameModel): Long {
        return frameDao.insertFrame(frame.toEntity())
    }

    override suspend fun updateFrame(frame: FrameModel) {
        frameDao.updateFrame(frame.toEntity())
    }

    override suspend fun insertFrames(frames: List<FrameModel>) {
        frameDao.insertFrames(frames.map { it.toEntity() })
    }

    override suspend fun deleteFrameById(id: Long) {
        frameDao.deleteFrameById(id)
    }

    override suspend fun deleteAllFrames() {
        frameDao.deleteAllFrames()
    }

    override suspend fun updateFrameOrder(id: Long, order: Int) {
        frameDao.updateFrameOrder(id, order)
    }

    // Layers

    override fun getAllLayers(frameId: Long): Flow<List<LayerModel>> {
        return layerDao.getAllLayers(frameId).map { it.map { it.toModel() } }
    }

    override suspend fun getLayerById(id: Long): LayerModel {
        return layerDao.getLayerById(id).toModel()
    }

    override suspend fun insertLayer(layer: LayerModel): Long {
        return layerDao.insertLayer(layer.toEntity())
    }

    override suspend fun updateLayer(layer: LayerModel) {
        layerDao.updateLayer(layer.toEntity())
    }

    override suspend fun insertLayers(layers: List<LayerModel>): List<Long> {
        return layerDao.insertLayers(layers.map { it.toEntity() })
    }

    override suspend fun deleteLayerById(id: Long) {
        layerDao.deleteLayerById(id)
    }

    override suspend fun deleteAllLayers() {
        layerDao.deleteAllLayers()
    }
}

fun ProjectEntity.toModel(): ProjectModel {
    return ProjectModel(
        id = id,
        name = name,
        thumbnail = thumbnail,
        created = created,
        lastModified = lastModified,
        frames = emptyList(),
        width = width,
        height = height,
    )
}

fun ProjectModel.toEntity(): ProjectEntity {
    return ProjectEntity(
        id = id,
        name = name,
        thumbnail = thumbnail,
        created = created,
        lastModified = lastModified,
        width = width,
        height = height,
    )
}

fun FrameEntity.toModel(): FrameModel {
    return FrameModel(
        id = id,
        projectId = projectId,
        name = name,
        order = order,
        layers = emptyList(),
    )
}

fun FrameModel.toEntity(): FrameEntity {
    return FrameEntity(
        id = id,
        projectId = projectId,
        name = name,
        order = order,
    )
}

fun LayerEntity.toModel(): LayerModel {
    return LayerModel(
        id = id,
        frameId = frameId,
        name = name,
        isVisible = isVisible,
        isLocked = isLocked,
        isBackground = isBackground,
        opacity = opacity,
        cachedBitmap = cachedBitmap,
        order = order,
        drawingPaths = emptyList(),
    )
}

fun LayerModel.toEntity(): LayerEntity {
    return LayerEntity(
        id = id,
        frameId = frameId,
        name = name,
        isVisible = isVisible,
        isLocked = isLocked,
        isBackground = isBackground,
        opacity = opacity,
        cachedBitmap = cachedBitmap,
        order = order,
    )
}