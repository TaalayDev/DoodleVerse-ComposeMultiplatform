package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Move
import com.composables.icons.lucide.MoveDiagonal2
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.RotateCw
import com.composables.icons.lucide.SignalZero
import io.github.taalaydev.doodleverse.core.SelectionHitTestResult
import io.github.taalaydev.doodleverse.core.SelectionState
import io.github.taalaydev.doodleverse.core.SelectionTransform
import io.github.taalaydev.doodleverse.core.hitTest
import io.github.taalaydev.doodleverse.core.toIntOffset

@Composable
fun SelectionOverlay(
    state: SelectionState,
    onTransformStart: (SelectionTransform, Offset) -> Unit,
    onTransformDelta: (Offset) -> Unit,
    onTransformEnd: () -> Unit,
    onTransform: ((centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit) = { _, _, _, _ -> },
    onTapOutside: () -> Unit,
    isMobile: Boolean,
    modifier: Modifier = Modifier
) {
    if (!state.isActive) return

    val bounds = state.getTransformedBounds()
    val handleSize = 16.dp

    val density = LocalDensity.current
    val pxToDp: (Int) -> Dp = { px ->
        with(density) { px.toDp() }
    }

    Box(
        modifier = modifier
            .pointerInput(state) {
                detectTapGestures {
                    val hitTestResult = state.hitTest(it)
                    if (hitTestResult == SelectionHitTestResult.Outside) {
                        onTapOutside()
                    }
                }
            }
    ) {
        // Resize handle
        Box(
            modifier = Modifier
                .offset { IntOffset(bounds.right.toInt(), bounds.bottom.toInt()) }
                .size(handleSize)
                .background(Color.White, CircleShape)
                .border(1.dp, Color.Black, CircleShape)
                .padding(4.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onTransformStart(SelectionTransform.Resize, offset)
                        },
                        onDrag = { _, dragAmount ->
                            onTransformDelta(dragAmount)
                        },
                        onDragEnd = {
                            onTransformEnd()
                        }
                    )
                }
        ) {
            Icon(
                imageVector = Lucide.MoveDiagonal2,
                contentDescription = null,
                modifier = Modifier.size(handleSize - 4.dp)
            )
        }

        // Rotation handle
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (bounds.center.x - 8.dp.toPx()).toInt(),
                        (bounds.top - 24.dp.toPx()).toInt()
                    )
                }
                .background(Color.White, CircleShape)
                .border(1.dp, Color.Gray, CircleShape)
                .padding(4.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onTransformStart(SelectionTransform.Rotate, offset)
                        },
                        onDrag = { _, dragAmount ->
                            onTransformDelta(dragAmount)
                        },
                        onDragEnd = {
                            onTransformEnd()
                        }
                    )
                }
        ) {
            Icon(
                imageVector = Lucide.RotateCcw,
                contentDescription = null,
                modifier = Modifier.size(handleSize - 4.dp)
            )
        }

        // Move handle
        Box(
            modifier = Modifier
                .width(pxToDp(bounds.size.width.toInt()))
                .height(pxToDp(bounds.size.height.toInt()))
                .offset { IntOffset(bounds.left.toInt(), bounds.top.toInt()) }
                .pointerInput(Unit, isMobile) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onTransformStart(SelectionTransform.Move, offset)
                        },
                        onDrag = { _, dragAmount ->
                            onTransformDelta(dragAmount)
                        },
                        onDragEnd = {
                            onTransformEnd()
                        }
                    )
                }
        ) {
            Icon(
                imageVector = Lucide.Move,
                contentDescription = null,
                modifier = Modifier.size(handleSize).align(Alignment.Center)
            )
        }
    }
}