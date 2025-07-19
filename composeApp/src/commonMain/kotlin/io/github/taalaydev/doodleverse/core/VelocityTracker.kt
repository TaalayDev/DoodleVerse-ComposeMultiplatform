package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap

data class VelocityTracker(
    var lastPosition: Offset = Offset.Zero,
    var lastTime: Long = 0L,
    var velocity: Float = 0f
) {
    fun updateVelocity(position: Offset, currentTime: Long): Float {
        if (lastTime != 0L) {
            val deltaTime = (currentTime - lastTime).coerceAtLeast(1)
            val distance = position.distanceTo(lastPosition)
            val currentVelocity = distance / (deltaTime / 1000f)

            velocity = velocity * 0.8f + currentVelocity * 0.2f
        }

        lastPosition = position
        lastTime = currentTime
        return velocity
    }

    fun calculateDynamicDistance(brushImage: ImageBitmap?, isShape: Boolean): Float {
        return when {
            brushImage != null || isShape -> 0f
            else -> {
                val normalizedVelocity = (velocity / 2000f).coerceIn(0f, 1f)
                val dynamicDistance = normalizedVelocity * 50f
                dynamicDistance.coerceIn(0f, 50f)
            }
        }
    }

    fun reset() {
        lastPosition = Offset.Zero
        lastTime = 0L
        velocity = 0f
    }
}
