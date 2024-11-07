package io.github.taalaydev.doodleverse

import io.github.taalaydev.doodleverse.shared.FrameModel
import io.github.taalaydev.doodleverse.shared.LayerModel
import io.github.taalaydev.doodleverse.shared.ProjectModel
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DemoProjectRepo : ProjectRepository() {
    private val projects = mutableListOf<ProjectModel>()
    private val frames = mutableListOf<FrameModel>()
    private val layers = mutableListOf<LayerModel>()

    override fun getAllProjects(): Flow<List<ProjectModel>> {
        return MutableStateFlow(projects)
    }

    override suspend fun getProjectById(id: Long): ProjectModel {
        return projects.first { it.id == id }
    }

    override suspend fun insertProject(project: ProjectModel): Long {
        val id = projects.size.toLong()
        projects.add(project.copy(id = id))
        insertFrames(project.frames)
        return id
    }

    override suspend fun updateProject(project: ProjectModel) {
        projects[projects.indexOfFirst { it.id == project.id }] = project
    }

    override suspend fun insertProjects(projects: List<ProjectModel>) {
        this.projects.addAll(projects)
    }

    override suspend fun deleteProjectById(id: Long) {
        projects.removeAll { it.id == id }
    }

    override suspend fun deleteAllProjects() {
        projects.clear()
    }

    override fun getAllFrames(projectId: Long): Flow<List<FrameModel>> {
        return MutableStateFlow(frames.filter { it.projectId == projectId })
    }

    override suspend fun getFrameById(id: Long): FrameModel {
        return frames.first { it.id == id }
    }

    override suspend fun insertFrame(frame: FrameModel): Long {
        val id = frames.size.toLong()
        frames.add(frame.copy(id = id))
        insertLayers(frame.layers)
        return id
    }

    override suspend fun updateFrame(frame: FrameModel) {
        frames[frames.indexOfFirst { it.id == frame.id }] = frame
    }

    override suspend fun insertFrames(frames: List<FrameModel>) {
        for (frame in frames) {
            insertFrame(frame)
        }
    }

    override suspend fun deleteFrameById(id: Long) {
        frames.removeAll { it.id == id }
    }

    override suspend fun deleteAllFrames() {
        frames.clear()
    }

    override suspend fun updateFrameOrder(id: Long, order: Int) {
        frames[frames.indexOfFirst { it.id == id }] = frames.first { it.id == id }.copy(order = order)
    }

    override fun getAllLayers(frameId: Long): Flow<List<LayerModel>> {
        return MutableStateFlow(layers.filter { it.frameId == frameId })
    }

    override suspend fun getLayerById(id: Long): LayerModel {
        return layers.first { it.id == id }
    }

    override suspend fun insertLayer(layer: LayerModel): Long {
        val id = layers.size.toLong()
        layers.add(layer.copy(id = id))
        return id
    }

    override suspend fun updateLayer(layer: LayerModel) {
        layers[layers.indexOfFirst { it.id == layer.id }] = layer
    }

    override suspend fun insertLayers(layers: List<LayerModel>): List<Long> {
        val ids = mutableListOf<Long>()
        for (layer in layers) {
            ids.add(insertLayer(layer))
        }
        return ids
    }

    override suspend fun deleteLayerById(id: Long) {
        layers.removeAll { it.id == id }
    }

    override suspend fun deleteAllLayers() {
        layers.clear()
    }
}