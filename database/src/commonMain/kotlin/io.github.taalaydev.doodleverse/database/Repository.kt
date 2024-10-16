package io.github.taalaydev.doodleverse.database

import io.github.taalaydev.doodleverse.data.database.dao.LayerDao
import io.github.taalaydev.doodleverse.data.database.dao.ProjectDao
import io.github.taalaydev.doodleverse.database.entities.ProjectEntity

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val layerDao: LayerDao,
) {
    fun getAllProjects() = projectDao.getAllProjects()
    fun getProjectById(id: Long) {}
    fun insertProject(project: ProjectEntity) {}
    fun updateProject(project: ProjectEntity) {}
    fun insertProjects(projects: List<ProjectEntity>) {}
    fun deleteProjectById(id: Long) {}
    fun deleteAllProjects() {}

    fun getAllLayers() {}
    fun getLayerById(id: Long) {}
    fun getLayersByProjectId(projectId: Long) {}
    fun insertLayer(layer: ProjectEntity) {}
    fun updateLayer(layer: ProjectEntity) {}
    fun insertLayers(layers: List<ProjectEntity>) {}
    fun deleteLayerById(id: Long) {}
    fun deleteAllLayers() {}
}