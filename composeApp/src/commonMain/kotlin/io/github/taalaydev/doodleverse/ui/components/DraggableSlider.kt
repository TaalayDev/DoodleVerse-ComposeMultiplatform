package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.GripHorizontal
import com.composables.icons.lucide.Lucide

@Composable
fun BoxScope.DraggableSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    initialPosition: IntOffset = IntOffset(10, 10),
    focusManager: androidx.compose.ui.focus.FocusManager? = null,
    keyboardController: SoftwareKeyboardController? = null,
    content: @Composable () -> Unit
) {
    var position by remember { mutableStateOf(initialPosition) }

    LaunchedEffect(initialPosition) {
        position = initialPosition
    }

    val boxModifier = Modifier
        .offset { position }
        .background(
            Color.White.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp)
        )
        .clip(RoundedCornerShape(8.dp))

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Icon(
                Lucide.GripHorizontal,
                contentDescription = "Move",
                tint = Color.Gray.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp).pointerInput(Unit) {
                    focusManager?.clearFocus()
                    keyboardController?.hide()

                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        position += IntOffset(
                            dragAmount.x.toInt(),
                            dragAmount.y.toInt()
                        )
                    }
                }
            )
            VerticalSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 5f..80f,
                modifier = Modifier.height(20.dp).width(200.dp),
            )
            content()
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}