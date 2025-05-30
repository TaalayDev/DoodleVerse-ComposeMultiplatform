package io.github.taalaydev.doodleverse.ui.screens.animation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taalaydev.doodleverse.data.models.AnimationStateModel
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.features.animation.AnimationExportFormat
import io.github.taalaydev.doodleverse.features.animation.AnimationLoopType
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


/**
 * Animation ViewModel that implements the business logic for the Animation Studio feature
 */
class AnimationViewModel(
    private val drawViewModel: DrawViewModel,
    private val projectId: Long
) : ViewModel() {

    // Animation state
    val project = drawViewModel.project

    private val _currentAnimationState = MutableStateFlow<AnimationStateModel?>(null)
    val currentAnimationState: StateFlow<AnimationStateModel?> = _currentAnimationState

    private val _frames = MutableStateFlow<List<FrameModel>>(emptyList())
    val frames: StateFlow<List<FrameModel>> = _frames

    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    // Onion skinning settings
    val onionSkinningEnabled = mutableStateOf(false)
    val prevOnionFrames = mutableStateOf(1)
    val nextOnionFrames = mutableStateOf(1)

    // Animation settings
    val loopType = mutableStateOf(AnimationLoopType.LOOP)

    private var playbackJob: Job? = null

    init {
        viewModelScope.launch {
            // Load the animation data
            val project = drawViewModel.project.value
            if (project != null) {
                // Get or create default animation state
                var animState = project.animationStates.firstOrNull()
//                if (animState == null) {
//                    // Create a default animation state if none exists
//                    val animationId = drawViewModel.createAnimationState("Animation 1")
//                    animState = drawViewModel.getAnimationState(animationId)
//                }
//
//                _currentAnimationState.value = animState
//                _frames.value = animState?.frames ?: emptyList()
//
//                // Initialize with at least one frame if empty
//                if (_frames.value.isEmpty()) {
//                    addFrame()
//                }
            }
        }
    }

    fun selectFrame(index: Int) {
        if (index in 0 until frames.value.size) {
            _currentFrameIndex.value = index
            // Update the drawing canvas to show this frame
            viewModelScope.launch {
                // drawViewModel.loadFrame(frames.value[index])
            }
        }
    }

    fun addFrame() {
        viewModelScope.launch {
            // Get current frame content to create a new frame
//            val newFrameOrder = frames.value.size
//            val frameId = drawViewModel.createFrame(
//                "Frame ${newFrameOrder + 1}",
//                _currentAnimationState.value?.id ?: 0,
//                newFrameOrder
//            )
//
//            val newFrame = drawViewModel.getFrame(frameId)
//            _frames.value = _frames.value + newFrame
//
//            // Select the new frame
//            _currentFrameIndex.value = newFrameOrder
//            drawViewModel.loadFrame(newFrame)
        }
    }

    fun deleteFrame(index: Int) {
        if (frames.value.size <= 1) {
            // Don't delete the last frame
            return
        }

        viewModelScope.launch {
            val frameToDelete = frames.value[index]
//            drawViewModel.deleteFrame(frameToDelete.id)
//
//            val updatedFrames = frames.value.toMutableList()
//            updatedFrames.removeAt(index)
//
//            // Update frame orders
//            updatedFrames.forEachIndexed { i, frame ->
//                if (frame.order != i) {
//                    drawViewModel.updateFrameOrder(frame.id, i)
//                }
//            }
//
//            _frames.value = updatedFrames
//
//            // Adjust current frame index if needed
//            if (_currentFrameIndex.value >= updatedFrames.size) {
//                _currentFrameIndex.value = maxOf(0, updatedFrames.size - 1)
//            }
//
//            // Load the new current frame
//            if (updatedFrames.isNotEmpty()) {
//                drawViewModel.loadFrame(updatedFrames[_currentFrameIndex.value])
//            }
        }
    }

    fun duplicateFrame(index: Int) {
        viewModelScope.launch {
//            val frameToDuplicate = frames.value[index]
//
//            // Create a new frame with the same content
//            val newFrameOrder = frames.value.size
//            val frameId = drawViewModel.duplicateFrame(
//                frameToDuplicate,
//                "Frame ${newFrameOrder + 1}",
//                newFrameOrder
//            )
//
//            val newFrame = drawViewModel.getFrame(frameId)
//            _frames.value = _frames.value + newFrame
//
//            // Select the new frame
//            _currentFrameIndex.value = newFrameOrder
//            drawViewModel.loadFrame(newFrame)
        }
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            // Stop playback
            playbackJob?.cancel()
            _isPlaying.value = false
        } else {
            // Start playback
            _isPlaying.value = true
            playbackJob = viewModelScope.launch {
                playAnimation()
            }
        }
    }

    private suspend fun playAnimation() {
        val frameCount = frames.value.size
        if (frameCount <= 1) return

        val fps = 12 // _currentAnimationState.value?.fps ?: 12
        val frameDuration = 1000L / fps // milliseconds per frame

        var currentIndex = _currentFrameIndex.value
        var direction = 1 // 1 for forward, -1 for backward (used in PING_PONG mode)

//        while (isActive) {
//            // Update current frame
//            currentIndex += direction
//
//            // Handle different loop types
//            when (loopType.value) {
//                AnimationLoopType.NONE -> {
//                    if (currentIndex >= frameCount) {
//                        // Stop at the end
//                        _isPlaying.value = false
//                        break
//                    }
//                }
//                AnimationLoopType.LOOP -> {
//                    if (currentIndex >= frameCount) {
//                        currentIndex = 0
//                    }
//                }
//                AnimationLoopType.PING_PONG -> {
//                    if (currentIndex >= frameCount) {
//                        direction = -1
//                        currentIndex = frameCount - 2
//                    } else if (currentIndex < 0) {
//                        direction = 1
//                        currentIndex = 1
//                    }
//                }
//            }
//
//            _currentFrameIndex.value = currentIndex
//            // drawViewModel.loadFrame(frames.value[currentIndex])
//
//            delay(frameDuration)
//        }
    }

    fun nextFrame() {
        val nextIndex = (_currentFrameIndex.value + 1) % frames.value.size
        selectFrame(nextIndex)
    }

    fun previousFrame() {
        val prevIndex = if (_currentFrameIndex.value > 0)
            _currentFrameIndex.value - 1
        else
            frames.value.size - 1

        selectFrame(prevIndex)
    }

    fun updateFps(fps: Int) {
        viewModelScope.launch {
            val currentState = _currentAnimationState.value ?: return@launch
            // drawViewModel.updateAnimationFps(currentState.id, fps)

            // _currentAnimationState.value = currentState.copy(fps = fps)
        }
    }

    fun updateLoopType(type: AnimationLoopType) {
        loopType.value = type
    }

    fun toggleOnionSkinning() {
        onionSkinningEnabled.value = !onionSkinningEnabled.value
        updateOnionSkinDisplay()
    }

    fun updatePrevOnionFrames(count: Int) {
        prevOnionFrames.value = count
        updateOnionSkinDisplay()
    }

    fun updateNextOnionFrames(count: Int) {
        nextOnionFrames.value = count
        updateOnionSkinDisplay()
    }

    private fun updateOnionSkinDisplay() {
        // Implementation to show/hide onion skin layers based on settings
        if (!onionSkinningEnabled.value) {
            // Hide all onion skinning layers
            return
        }

        val currentIndex = currentFrameIndex.value
        val framesList = frames.value

        // Show previous frames with decreasing opacity
        for (i in 1..prevOnionFrames.value) {
            val prevIndex = currentIndex - i
            if (prevIndex >= 0) {
                // Show previous frame with red tint and lower opacity
                // Implementation depends on how layers are managed in the DrawViewModel
            }
        }

        // Show next frames with decreasing opacity
        for (i in 1..nextOnionFrames.value) {
            val nextIndex = currentIndex + i
            if (nextIndex < framesList.size) {
                // Show next frame with blue tint and lower opacity
            }
        }
    }

    fun calculateDuration(): Float {
        val fps = 12 // _currentAnimationState.value?.fps ?: 12
        val frameCount = frames.value.size
        return frameCount.toFloat() / fps
    }

    fun exportAnimation(
        format: AnimationExportFormat,
        quality: Int,
        resolution: Float
    ) {
        viewModelScope.launch {
            // Implementation to render and export the animation

            // 1. Render each frame at the specified resolution
            val frameImages = frames.value.map { frame ->
                // Render frame to bitmap
                // This would use drawViewModel to render each frame
                // Convert to specified resolution
                ImageBitmap(100, 100) // Placeholder
            }

            // 2. Generate the output file based on selected format
            when (format) {
                AnimationExportFormat.GIF -> {
                    // Create animated GIF
                }
                AnimationExportFormat.MP4 -> {
                    // Create MP4 video
                }
                AnimationExportFormat.WEBP -> {
                    // Create animated WebP
                }
                AnimationExportFormat.PNG_SEQUENCE -> {
                    // Create sequence of PNG files
                }
            }

            // 3. Save the file
            // Implementation depends on platform (Android, iOS, Desktop)

            // 4. Notify user of completion
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
    }
}