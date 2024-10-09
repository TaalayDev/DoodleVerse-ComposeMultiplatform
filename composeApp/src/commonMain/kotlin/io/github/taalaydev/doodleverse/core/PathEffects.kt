package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StampedPathEffectStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object PathEffects {
    private fun toRadians(degrees: Double): Double = degrees * PI / 180

    fun createStarPathEffect(
        width: Float,
        points: Int = 5,
        advance: Float = 10f,
        innerRadius: Float = 0.5f,
        outerRadius: Float = 1.0f
    ): PathEffect {
        val path = Path()
        val half = 360f / points / 2
        val outer = outerRadius * width / 2
        val inner = innerRadius * width / 2
        val startAngle = -90f
        val angle = 360f / points
        path.moveTo(
            outer * cos(toRadians(startAngle.toDouble())).toFloat(),
            outer * sin(toRadians(startAngle.toDouble())).toFloat()
        )
        for (i in 1 until points * 2) {
            val radius = if (i % 2 == 0) outer else inner
            val angleInRadians = toRadians((startAngle + i * angle).toDouble())
            path.lineTo(
                radius * cos(angleInRadians).toFloat(),
                radius * sin(angleInRadians).toFloat()
            )
        }
        path.close()
        return PathEffect.stampedPathEffect(path, advance, 10f, StampedPathEffectStyle.Translate)
    }

    fun markerPathEffect(width: Float): PathEffect = PathEffect.stampedPathEffect(
        Path().apply {
            moveTo(0f, 0f)
            lineTo(width - 10f, width)
            lineTo(width, width - 10f)
            lineTo(10f, 0f)
            lineTo(0f, 10f)
            close()
        },
        1f, 10f, StampedPathEffectStyle.Translate
    )

    fun spiralPathEffect(width: Float, turns: Int = 5): PathEffect {
        val spiralPath = Path()
        val stepAngle = (2 * PI) / 36 // Approx 36 steps per loop for smooth spiral
        var currentRadius = 0f
        var angle = 0.0

        // Generate spiral path
        while (currentRadius < width * 2) {
            val x = (currentRadius * cos(angle)).toFloat()
            val y = (currentRadius * sin(angle)).toFloat()
            if (angle == 0.0) {
                spiralPath.moveTo(x, y) // Start the path
            } else {
                spiralPath.lineTo(x, y) // Draw the spiral line
            }

            // Increase the radius and angle for the next point in the spiral
            currentRadius += width / 36
            angle += stepAngle
        }

        // Create a PathEffect using the generated spiral path
        return PathEffect.stampedPathEffect(
            spiralPath, advance = width * 2, phase = 0f, style = StampedPathEffectStyle.Translate
        )
    }

    fun zigzagPathEffect(width: Float, amplitude: Float = 10f): PathEffect {
        val path = Path().apply {
            var upward = true
            for (i in 0..360 step 20) {
                val x = i * width / 360
                val y = if (upward) amplitude else -amplitude
                lineTo(x, y)
                upward = !upward
            }
        }
        return PathEffect.stampedPathEffect(path, 20f, 10f, StampedPathEffectStyle.Translate)
    }

    fun createBubblePathEffect(width: Float): PathEffect {
        val path = Path()
        path.addOval(Rect(0f, 0f, width, width))

        return PathEffect.stampedPathEffect(path, width * 1.2f, width * 0.2f, StampedPathEffectStyle.Translate)
    }

    fun createLeafPathEffect(width: Float): PathEffect {
        val path = Path()
        path.moveTo(0f, 0f)
        path.cubicTo(width / 2, -width / 2, width / 2, width / 2, 0f, 0f)
        path.cubicTo(-width / 2, width / 2, -width / 2, -width / 2, 0f, 0f)

        return PathEffect.stampedPathEffect(path, width * 1.5f, 0f, StampedPathEffectStyle.Rotate)
    }

    fun wavePathEffect(width: Float, amplitude: Float = 10f, frequency: Float = 2f): PathEffect {
        val path = Path().apply {
            for (i in 0..360 step 10) {
                val x = i * width / 360
                val y = amplitude * sin(toRadians((i * frequency).toDouble())).toFloat()
                lineTo(x, y)
            }
        }
        return PathEffect.stampedPathEffect(path, 20f, 10f, StampedPathEffectStyle.Translate)
    }

    fun heartPathEffect(width: Float): PathEffect {
        val path = Path().apply {
            moveTo(width / 2, width / 5)
            cubicTo(width, 0f, width, width / 2, width / 2, width)
            cubicTo(0f, width / 2, 0f, 0f, width / 2, width / 5)
        }
        return PathEffect.stampedPathEffect(path, 40f, 10f, StampedPathEffectStyle.Translate)
    }

    fun sketchyPencilPathEffect(width: Float): PathEffect {
        val path = Path().apply {
            var upward = true
            for (i in 0..360 step 20) {
                val x = i * width / 360
                val y = if (upward) randomOffset(width, 2f) else randomOffset(width, -2f)
                lineTo(x, y)
                upward = !upward
            }
        }

        // Combine a dashed effect with the path to simulate uneven pencil marks
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)

        // Apply the sketchy effect by chaining the path with a wobble
        return PathEffect.chainPathEffect(dashEffect, PathEffect.stampedPathEffect(path, 15f, 5f, StampedPathEffectStyle.Translate))
    }

    fun randomOffset(width: Float, baseOffset: Float): Float {
        return baseOffset + Random.nextFloat() * width / 10
    }

}