package io.github.taalaydev.doodleverse.ui.screens.lesson

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taalaydev.doodleverse.ImageFormat
import io.github.taalaydev.doodleverse.engine.controller.DrawOperations
import io.github.taalaydev.doodleverse.data.models.AnimationStateModel
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.toEntity
import io.github.taalaydev.doodleverse.engine.DrawingState
import io.github.taalaydev.doodleverse.engine.controller.DrawEngineController
import io.github.taalaydev.doodleverse.imageBitmapByteArray
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

class LessonDrawViewModel(
    private val projectRepo: ProjectRepository,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    @OptIn(ExperimentalTime::class)
    private val drawingOperations = object : DrawOperations {
        override suspend fun addLayer(layer: LayerModel): Long {
            return kotlin.time.Clock.System.now().toEpochMilliseconds()
        }
        override suspend fun deleteLayer(layer: LayerModel) {}
        override suspend fun updateLayer(layer: LayerModel, bitmap: ImageBitmap?) {}
        override suspend fun saveProject() {}
    }

    // Drawing controller with focused responsibilities
    val drawController = DrawEngineController(
        operations = drawingOperations,
        scope = viewModelScope,
        dispatcher = dispatcher,
        initialBrushSize = 5f
    )

    // Delegate properties to controller
    val state: StateFlow<DrawingState> = drawController.state

    /**
     * Creates a project from the current lesson drawing
     */
    fun createProject(
        name: String,
        width: Int,
        height: Int,
        onProjectCreated: (ProjectModel) -> Unit
    ) {
        viewModelScope.launch(dispatcher) {
            try {
                // Get the current drawing bitmap
                val combinedBitmap = drawController.getCombinedBitmap()
                val thumbnailBytes = combinedBitmap?.let {
                    imageBitmapByteArray(it, ImageFormat.PNG)
                }

                // Create project structure
                val project = ProjectModel(
                    id = 0,
                    name = name,
                    animationStates = emptyList(),
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    lastModified = Clock.System.now().toEpochMilliseconds(),
                    aspectRatio = Size(width.toFloat(), height.toFloat()),
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
                )

                // Save to database
                val projectId = projectRepo.insertProject(
                    project.toEntity().copy(thumb = thumbnailBytes)
                )

                val animationStateId = projectRepo.insertAnimationState(
                    animationState.copy(projectId = projectId).toEntity()
                )

                val frameId = projectRepo.insertFrame(
                    frame.copy(animationId = animationStateId).toEntity()
                )

                val layerId = projectRepo.insertLayer(
                    layer.copy(frameId = frameId).toEntity().copy(
                        pixels = thumbnailBytes ?: byteArrayOf(),
                        width = width,
                        height = height
                    )
                )

                // Return the complete project
                viewModelScope.launch(Dispatchers.Main) {
                    onProjectCreated(
                        project.copy(
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
                    )
                }

            } catch (e: Exception) {
                println("Error creating project from lesson: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        drawController.cleanup()
    }
}