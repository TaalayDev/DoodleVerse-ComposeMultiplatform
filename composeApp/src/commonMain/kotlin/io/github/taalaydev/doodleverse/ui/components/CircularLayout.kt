package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun radians(degrees: Double): Double = degrees * PI / 180

@Composable
fun CircularLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val radius = constraints.maxWidth / 2
        val centerX = constraints.maxWidth / 2
        val centerY = constraints.maxHeight / 2

        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        val itemCount = placeables.size
        val angleBetweenItems = 360f / itemCount

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val angleInDegrees = angleBetweenItems * index - 90f
                val angleInRadians = radians(angleInDegrees.toDouble())
                val x = (centerX + radius * cos(angleInRadians) - placeable.width / 2).toInt()
                val y = (centerY + radius * sin(angleInRadians) - placeable.height / 2).toInt()
                placeable.place(x, y)
            }
        }
    }
}
