package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
) {
    val thumbRadius = 12.dp
    val trackHeight = 4.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thumbRadius * 2)
    ) {
        // Track
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(trackHeight / 2))
                .height(trackHeight)
                .fillMaxWidth()
                .background(inactiveColor)
                .align(Alignment.Center)
        )

        // Active track
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(trackHeight / 2))
                .height(trackHeight)
                .fillMaxWidth(value)
                .background(activeColor)
                .align(Alignment.CenterStart)
        )

        // Thumb
        var thumbPosition by remember { mutableStateOf(value) }
        val thumbOffset by animateFloatAsState(thumbPosition)

        Box(
            modifier = Modifier
                .offset { IntOffset((thumbOffset * 100).toInt(), 0) }
                .size(thumbRadius * 2)
                .background(Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            thumbPosition = value
                        },
                        onDrag = { change, dragAmount ->
                            val newValue = (thumbPosition + dragAmount.x / size.width).coerceIn(0f, 1f)
                            thumbPosition = newValue
                            onValueChange(newValue)
                        }
                    )
                }
                .align(Alignment.CenterStart)
        )
    }
}