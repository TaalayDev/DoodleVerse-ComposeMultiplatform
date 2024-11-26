package io.github.taalaydev.doodleverse.ui.screens.home

import androidx.compose.ui.geometry.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.toEntity
import io.github.taalaydev.doodleverse.data.models.toModel
import io.github.taalaydev.doodleverse.data.models.AnimationStateModel
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class HomeViewModel(
    private val repository: ProjectRepository
) : ViewModel() {
    private val _projects = MutableStateFlow<List<ProjectModel>>(emptyList())
    val projects: StateFlow<List<ProjectModel>> = _projects.asStateFlow()

    suspend fun loadProjects() {
        repository.getAllProjects().collect {
            _projects.value = it.map { project -> project.toModel() }
        }
    }

    suspend fun createProject(name: String, width: Float, height: Float): ProjectModel {
        val project = ProjectModel(
            id = 0,
            name = name,
            animationStates = emptyList(),
            createdAt = Clock.System.now().toEpochMilliseconds(),
            lastModified = Clock.System.now().toEpochMilliseconds(),
            aspectRatio = Size(width, height),
        )
        val animationState = AnimationStateModel(
            id = 0,
            name = "Animation 1",
            duration = 1000,
            frames = emptyList(),
            projectId = 0,
        )
        val frame = FrameModel(
            id = 0,
            animationId = 0,
            name = "Frame 1",
            layers = emptyList(),
        )
        val layer = LayerModel(
            id = 0,
            frameId = 0,
            name = "Layer 1",
            isVisible = true,
            isLocked = false,
            isBackground = false,
            opacity = 1.0,
            paths = emptyList(),
        )

        val projectId = repository.insertProject(project.toEntity())
        val animationStateId = repository.insertAnimationState(animationState.copy(projectId = projectId).toEntity())
        val frameId = repository.insertFrame(frame.copy(animationId = animationStateId).toEntity())
        val layerId = repository.insertLayer(layer.copy(frameId = frameId).toEntity())

        return project.copy(
            id = projectId,
            animationStates = listOf(
                animationState.copy(
                    id = animationStateId,
                    frames = listOf(
                        frame.copy(
                            id = frameId,
                            layers = listOf(
                                layer.copy(id = layerId)
                            )
                        )
                    )
                )
            )
        )
    }
}