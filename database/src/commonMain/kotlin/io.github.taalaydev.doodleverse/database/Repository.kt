package io.github.taalaydev.doodleverse.database

import io.github.taalaydev.doodleverse.data.database.dao.DrawingPathDao
import io.github.taalaydev.doodleverse.data.database.dao.LayerDao
import io.github.taalaydev.doodleverse.data.database.dao.PointDao
import io.github.taalaydev.doodleverse.data.database.dao.ProjectDao
import io.github.taalaydev.doodleverse.database.dao.AnimationStateDao
import io.github.taalaydev.doodleverse.database.dao.FrameDao
import io.github.taalaydev.doodleverse.database.entities.AnimationStateEntity
import io.github.taalaydev.doodleverse.database.entities.DrawingPathEntity
import io.github.taalaydev.doodleverse.database.entities.LayerEntity
import io.github.taalaydev.doodleverse.database.entities.ProjectEntity
import io.github.taalaydev.doodleverse.database.entities.FrameEntity
import io.github.taalaydev.doodleverse.database.entities.PointEntity
import io.github.taalaydev.doodleverse.shared.AnimationStateModel
import io.github.taalaydev.doodleverse.shared.DrawingPathModel
import io.github.taalaydev.doodleverse.shared.FrameModel
import io.github.taalaydev.doodleverse.shared.LayerModel
import io.github.taalaydev.doodleverse.shared.PointModel
import io.github.taalaydev.doodleverse.shared.ProjectModel
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ProjectRepositoryImpl(
    private val projectDao: ProjectDao,
    private val animationStateDao: AnimationStateDao,
    private val frameDao: FrameDao,
    private val layerDao: LayerDao,
    private val drawingPathDao: DrawingPathDao,
    private val pointDao: PointDao,
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

    // Animation States

    override suspend fun getAllAnimationStates(projectId: Long): List<AnimationStateModel> {
        return animationStateDao.getAll(projectId).map {
            it.toModel().copy(
                frames = frameDao.getAllFrames(it.id).map { it.toModel() }
            )
        }
    }

    override suspend fun getAnimationStateById(id: Long): AnimationStateModel {
        return animationStateDao.getById(id).toModel()
    }

    override suspend fun insertAnimationState(animationState: AnimationStateModel): Long {
        return animationStateDao.insert(animationState.toEntity())
    }

    override suspend fun updateAnimationState(animationState: AnimationStateModel) {
        animationStateDao.update(animationState.toEntity())
    }

    override suspend fun insertAnimationStates(animationStates: List<AnimationStateModel>) {
        animationStateDao.insert(animationStates.map { it.toEntity() })
    }

    override suspend fun deleteAnimationStateById(id: Long) {
        animationStateDao.deleteById(id)
    }

    override suspend fun deleteAllAnimationStates() {
        animationStateDao.deleteAll()
    }

    // Frames

    override suspend fun getAllFrames(animationStateId: Long): List<FrameModel> {
        return frameDao.getAllFrames(animationStateId).map {
            it.toModel().copy(
                layers = layerDao.getAllLayers(it.id).map { it.toModel() }
            )
        }
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

    override suspend fun getAllLayers(frameId: Long): List<LayerModel> {
        return layerDao.getAllLayers(frameId).map { it.toModel() }
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

    // Drawing Paths

    override suspend fun getAllDrawingPaths(layerId: Long): List<DrawingPathModel> {
        val paths = drawingPathDao.getDrawingPathsByLayerId(layerId).map { it.toModel() }
        return paths.map { path ->
            path.copy(points = getAllPoints(path.id))
        }
    }

    override suspend fun getDrawingPathById(id: Long): DrawingPathModel {
        return drawingPathDao.getDrawingPathById(id).toModel()
    }

    override suspend fun insertDrawingPath(drawingPath: DrawingPathModel): Long {
        val id = drawingPathDao.insertDrawingPath(drawingPath.toEntity())

        val points = drawingPath.points.map { it.copy(drawingPathId = id).toEntity() }
        pointDao.insertPoints(points)

        return id
    }

    override suspend fun updateDrawingPath(drawingPath: DrawingPathModel) {
        drawingPathDao.updateDrawingPath(drawingPath.toEntity())
    }

    override suspend fun insertDrawingPaths(drawingPaths: List<DrawingPathModel>): List<Long> {
        return drawingPathDao.insertDrawingPaths(drawingPaths.map { it.toEntity() })
    }

    override suspend fun deleteDrawingPathById(id: Long) {
        drawingPathDao.deleteDrawingPathById(id)
    }

    override suspend fun deleteAllDrawingPaths() {
        drawingPathDao.deleteAllDrawingPaths()
    }

    // Points

    override suspend fun getAllPoints(drawingPathId: Long): List<PointModel> {
        return pointDao.getPointsByPathId(drawingPathId).map { it.toModel() }
    }

    override suspend fun getPointById(id: Long): PointModel {
        return pointDao.getPointById(id).toModel()
    }

    override suspend fun insertPoint(point: PointModel): Long {
        return pointDao.insertPoint(point.toEntity())
    }

    override suspend fun updatePoint(point: PointModel) {
        pointDao.updatePoint(point.toEntity())
    }

    override suspend fun insertPoints(points: List<PointModel>): List<Long> {
        return pointDao.insertPoints(points.map { it.toEntity() })
    }

    override suspend fun deletePointById(id: Long) {
        pointDao.deletePointById(id)
    }

    override suspend fun deleteAllPoints() {
        pointDao.deleteAllPoints()
    }
}

fun ProjectEntity.toModel(): ProjectModel {
    return ProjectModel(
        id = id,
        name = name,
        thumbnail = thumbnail,
        created = created,
        lastModified = lastModified,
        animationStates = emptyList(),
        width = width,
        height = height,
        thumb = thumb,
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
        thumb = thumb,
    )
}

fun AnimationStateEntity.toModel(): AnimationStateModel {
    return AnimationStateModel(
        id = id,
        name = name,
        duration = duration,
        frames = emptyList(),
        projectId = projectId,
    )
}

fun AnimationStateModel.toEntity(): AnimationStateEntity {
    return AnimationStateEntity(
        id = id,
        name = name,
        duration = duration,
        projectId = projectId,
    )
}

fun FrameEntity.toModel(): FrameModel {
    return FrameModel(
        id = id,
        animationId = animationId,
        name = name,
        order = order,
        layers = emptyList(),
    )
}

fun FrameModel.toEntity(): FrameEntity {
    return FrameEntity(
        id = id,
        animationId = animationId,
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
        pixels = pixels,
        width = width,
        height = height,
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
        pixels = pixels,
        width = width,
        height = height,
    )
}

fun DrawingPathModel.toEntity(): DrawingPathEntity {
    return DrawingPathEntity(
        id = id,
        layerId = layerId,
        brushId = brushId,
        color = color,
        strokeWidth = strokeWidth,
        randoms = randoms,
        startPointX = startPointX,
        startPointY = startPointY,
        endPointX = endPointX,
        endPointY = endPointY,
    )
}

fun DrawingPathEntity.toModel(): DrawingPathModel {
    return DrawingPathModel(
        id = id,
        layerId = layerId,
        brushId = brushId,
        color = color,
        strokeWidth = strokeWidth,
        points = emptyList(),
        randoms = randoms,
        startPointX = startPointX,
        startPointY = startPointY,
        endPointX = endPointX,
        endPointY = endPointY,
    )
}

fun PointModel.toEntity(): PointEntity {
    return PointEntity(
        id = id,
        drawingPathId = drawingPathId,
        x = x,
        y = y,
    )
}

fun PointEntity.toModel(): PointModel {
    return PointModel(
        id = id,
        drawingPathId = drawingPathId,
        x = x,
        y = y,
    )
}