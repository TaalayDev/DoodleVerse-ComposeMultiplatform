package io.github.taalaydev.doodleverse.engine.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taalaydev.doodleverse.engine.DragState
import io.github.taalaydev.doodleverse.engine.brush.PenBrush
import io.github.taalaydev.doodleverse.engine.controller.DrawEngineController
import io.github.taalaydev.doodleverse.engine.util.modDetectTransformGestures

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun DrawBox(
    controller: DrawEngineController,
    dragState: MutableState<DragState>,
    background: Color = Color.White,
    referenceImage: ImageBitmap? = null,
    referenceBlendMode: BlendMode = BlendMode.SrcOver,
    modifier: Modifier = Modifier,
) {
    val currentTool by controller.currentTool.collectAsStateWithLifecycle()

    val size = calculateWindowSizeClass()
    val isMobile = when (size.widthSizeClass) {
        WindowWidthSizeClass.Compact -> true
        else -> false
    }
    val isTablet = when (size.widthSizeClass) {
        WindowWidthSizeClass.Medium -> true
        else -> false
    }

    val imageSize = controller.imageSize
    val aspectRatio = remember(imageSize) {
        if (imageSize.height == 0 || imageSize.width == 0) {
            1f
        } else {
            imageSize.width.toFloat() / imageSize.height.toFloat()
        }
    }

    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isPressed, isFocused) {
        if (!isFocused) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = dragState.value.zoom
                scaleY = dragState.value.zoom
                translationX = dragState.value.draggedTo.x
                translationY = dragState.value.draggedTo.y
                rotationZ = dragState.value.rotation
            }
            .padding(10.dp)
            .focusRequester(focusRequester)
            .focusable(
                interactionSource = interactionSource
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val isControlPressed = event.isCtrlPressed || event.isMetaPressed
                    val isShiftPressed = event.isShiftPressed
                    val isSpacePressed = event.key == Key.Spacebar

                    when {
                        isControlPressed && !isShiftPressed && event.key == Key.Z -> {
                            controller.undo()
                            true
                        }
                        isControlPressed && isShiftPressed && event.key == Key.Z -> {
                            controller.redo()
                            true
                        }
                        isControlPressed && event.key == Key.B -> {
                            true
                        }
                        isControlPressed && (event.key == Key.Plus || event.key == Key.Equals) -> {
                            dragState.value = dragState.value.copy(
                                zoom = (dragState.value.zoom * 1.1f).coerceAtMost(5f)
                            )
                            true
                        }
                        isControlPressed && event.key == Key.Minus -> {
                            dragState.value = dragState.value.copy(
                                zoom = (dragState.value.zoom / 1.1f).coerceAtLeast(0.2f)
                            )
                            true
                        }
                        isSpacePressed -> {
                            true
                        }
                        event.key == Key.Enter -> {
                            controller.applySelection(); true
                        }
                        event.key == Key.Escape -> {
                            controller.clearSelection(); true
                        }
                        else -> {
                            false
                        }
                    }
                } else {
                    false
                }
            }
            .pointerInput(currentTool, isMobile) {
                if (!isMobile) {
                    return@pointerInput awaitEachGesture {
                        val event = awaitPointerEvent()
                        if (event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed) {
                            val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            val scrollX = event.changes.firstOrNull()?.scrollDelta?.x ?: 0f

                            if (scrollY != 0f || scrollX != 0f) {
                                val sensitivity = 10f
                                dragState.value = dragState.value.copy(
                                    draggedTo = dragState.value.draggedTo.copy(
                                        y = dragState.value.draggedTo.y - scrollY * sensitivity,
                                        x = dragState.value.draggedTo.x - scrollX * sensitivity
                                    )
                                )
                            }
                        } else {
                            val scrollDelta =
                                event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (scrollDelta != 0f) {
                                val sensitivity = 0.01f
                                val zoomChange = 1f + (scrollDelta * sensitivity)
                                dragState.value = dragState.value.copy(
                                    zoom = (dragState.value.zoom * zoomChange).coerceIn(
                                        0.5f,
                                        5f
                                    )
                                )
                            }
                        }
                    }
                }
                modDetectTransformGestures { _, pan, zoom, rotation ->
                    dragState.value = dragState.value.copy(
                        zoom = (dragState.value.zoom * zoom).coerceIn(0.5f, 5f),
                        draggedTo = dragState.value.draggedTo + pan,
                        rotation = dragState.value.rotation + rotation
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(aspectRatio)
                .background(background)
        )

        DrawCanvas(
            controller = controller,
            gestureEnabled = !currentTool.isZoom && !currentTool.isDrag,
            onColorPicked = { color ->
                controller.setColor(color)
            },
            modifier = Modifier.aspectRatio(aspectRatio),
            referenceImage = referenceImage,
            referenceBlendMode = referenceBlendMode,
        )
    }
}