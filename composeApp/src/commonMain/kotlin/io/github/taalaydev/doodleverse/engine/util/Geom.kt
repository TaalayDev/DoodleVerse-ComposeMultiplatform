package io.github.taalaydev.doodleverse.engine.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Region updated by a brush step; null means "unknown" — caller may invalidate all. */
typealias DirtyRect = Rect?

fun Rect.expand(pad: Float): Rect = Rect(
    left - pad, top - pad, right + pad, bottom + pad
)

fun Rect.expandToInclude(other: Rect): Rect = Rect(
    min(this.left, other.left),
    min(this.top, other.top),
    max(this.right, other.right),
    max(this.bottom, other.bottom)
)

fun Offset.Companion.lerp(prev: Offset, next: Offset, t: Float): Offset {
    val x = prev.x + (next.x - prev.x) * t
    val y = prev.y + (next.y - prev.y) * t
    return Offset(x, y)
}

fun Offset.lerp(other: Offset, t: Float): Offset {
    val x = this.x + (other.x - this.x) * t
    val y = this.y + (other.y - this.y) * t
    return Offset(x, y)
}

fun Offset.distanceTo(other: Offset): Float {
    val dx = other.x - this.x
    val dy = other.y - this.y
    return kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
}

/** Utility to merge two DirtyRects. */
fun DirtyRect.union(other: DirtyRect): DirtyRect = when {
    this == null -> other
    other == null -> this
    else -> Rect(
        left = minOf(this.left, other.left),
        top = minOf(this.top, other.top),
        right = maxOf(this.right, other.right),
        bottom = maxOf(this.bottom, other.bottom),
    )
}

fun toDegrees(radians: Double): Double = radians * (180.0 / PI)
fun toRadians(degrees: Double): Double = degrees * (PI / 180.0)

fun kotlin.random.Random.nextGaussian(): Double {
    var v1: Double
    var v2: Double
    var s: Double
    do {
        v1 = 2 * nextDouble() - 1
        v2 = 2 * nextDouble() - 1
        s = v1 * v1 + v2 * v2
    } while (s >= 1 || s == 0.0)
    val multiplier = sqrt(-2 * ln(s) / s)
    return v1 * multiplier
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (start * (1 - fraction) + stop * fraction)
}

internal fun Offset.getDensityOffsetBetweenPoints(
    startPoint: Offset,
    density: Float = 10f,
    handleOffset: (Offset) -> Unit
) {
    val dx = this.x - startPoint.x
    val dy = this.y - startPoint.y

    // Use Manhattan distance for faster calculation
    val distance = abs(dx) + abs(dy)

    // Early exit if distance is too small
    if (distance < density) {
        handleOffset(this)
        return
    }

    val steps = (distance / density).toInt()
    val stepX = dx / steps
    val stepY = dy / steps

    var x = startPoint.x
    var y = startPoint.y

    repeat(steps) {
        handleOffset(Offset(x, y))
        x += stepX
        y += stepY
    }

    // Handle the last point
    handleOffset(this)
}

private fun quadraticBezier(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val x = (1 - t).pow(2) * p0.x + 2 * (1 - t) * t * p1.x + t.pow(2) * p2.x
    val y = (1 - t).pow(2) * p0.y + 2 * (1 - t) * t * p1.y + t.pow(2) * p2.y
    return Offset(x, y)
}

internal fun Offset.getDistance(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}

internal fun distanceBetween(a: Offset, b: Offset) = sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
internal fun centerOf(a: Offset, b: Offset) = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)