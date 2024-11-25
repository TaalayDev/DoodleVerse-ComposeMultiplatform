package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.taalaydev.doodleverse.core.DragState
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.core.handleDragAndZoomGestures
import io.github.taalaydev.doodleverse.core.modDetectTransformGestures
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawProvider
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawingController
import org.jetbrains.compose.resources.painterResource

@Composable
fun ColumnScope.DrawBox(
    drawProvider: DrawProvider,
    drawController: DrawingController,
    currentBrush: BrushData,
    currentColor: Color,
    brushSize: Float,
    currentTool: Tool,
    isMobile: Boolean,
    dragState: MutableState<DragState>,
    aspectRatio: Float,
    background: Color = Color.White,
    referenceImage: Painter? = null,
) {
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
        modifier = Modifier
            .weight(1f)
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
                            drawProvider.undo()
                            true
                        }
                        isControlPressed && isShiftPressed && event.key == Key.Z -> {
                            drawProvider.redo()
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
                        else -> {
                            false
                        }
                    }
                } else {
                    false
                }
            }
            .pointerInput(currentTool, isMobile) {
                if (currentTool == Tool.Drag) {
                    return@pointerInput detectDragGestures(
                        onDragStart = { offset ->
                            drawProvider.startMove(offset)
                        },
                        onDrag = { change, _ ->
                            drawProvider.updateMove(change.position)
                        },
                        onDragEnd = {
                            drawProvider.endMove()
                        }
                    )
                }
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
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(aspectRatio)
                .background(background, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
        )

        if (referenceImage != null) {
            Image(
                painter = referenceImage,
                contentDescription = null,
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.5f),
                colorFilter = ColorFilter.tint(Color.Red)
            )
        }

        DrawCanvas(
            provider = drawProvider,
            currentBrush = currentBrush,
            currentColor = currentColor,
            brushSize = brushSize,
            tool = currentTool,
            gestureEnabled = !currentTool.isZoom && !currentTool.isDrag,
            controller = drawController,
            onColorPicked = { color ->
                drawProvider.setColor(color)
            },
            modifier = Modifier.aspectRatio(aspectRatio),
        )
    }
}