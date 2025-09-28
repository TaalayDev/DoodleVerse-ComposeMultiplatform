package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.TextureBrush
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlinx.datetime.Clock
import kotlin.math.*
import kotlin.random.Random

/**
 * Configuration for texture stamp brush behavior
 */
@Immutable
data class TextureStampConfig(
    val rotationMode: RotationMode = RotationMode.STROKE_DIRECTION,
    val scaleVariation: Float = 0.1f, // 0-1, amount of random scale variation
    val opacityVariation: Float = 0.15f, // 0-1, amount of random opacity variation
    val scatterRadius: Float = 0f, // pixels, random position offset
    val pressureAffectsSize: Boolean = true,
    val pressureAffectsOpacity: Boolean = true,
    val velocityAffectsSize: Boolean = false,
    val flipRandomly: Boolean = false,
    val rotationRandomness: Float = 0f // 0-360 degrees of random rotation
)

enum class RotationMode {
    NONE, // No rotation
    STROKE_DIRECTION, // Align with stroke direction
    FIXED_ANGLE, // Fixed angle (use brush angle parameter)
    RANDOM, // Random rotation each stamp
    FOLLOW_PATH // Smoothly follow path direction
}

/**
 * Professional texture stamp brush with advanced stamping features:
 * - Multiple rotation modes (direction-based, fixed, random)
 * - Pressure and velocity sensitivity
 * - Scale and opacity variations
 * - Scatter/jitter effects
 * - Support for multiple textures with randomization
 * - Efficient spacing-based stamping
 */
class TextureStampBrush(
    override val texture: ImageBitmap,
    private val additionalTextures: List<ImageBitmap> = emptyList(),
    private val config: TextureStampConfig = TextureStampConfig(),
    private val fixedAngle: Float = 0f, // Used when rotationMode is FIXED_ANGLE
    override val id: ToolId = ToolId("texture_stamp"),
    override val name: String = "Texture Stamp"
) : TextureBrush() {

    // All available textures (primary + additional)
    private val allTextures = listOf(texture) + additionalTextures

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = TextureStampStrokeSession(canvas, allTextures, config, fixedAngle, params)
}

private class TextureStampStrokeSession(
    private val canvas: Canvas,
    private val textures: List<ImageBitmap>,
    private val config: TextureStampConfig,
    private val fixedAngle: Float,
    params: BrushParams
) : StrokeSession(params) {

    // Path tracking for direction calculation
    private var lastPosition: Offset = Offset.Zero
    private var currentPosition: Offset = Offset.Zero
    private var strokeDirection: Float = 0f // in degrees

    // Spacing system
    private val spacing: Float = params.size * 0.2f
    private var residualDistance: Float = 0f

    // Random number generator with consistent seed per stroke
    private val random: Random = Random(params.hashCode() + Clock.System.now().toEpochMilliseconds().toInt())

    // Paint for texture rendering
    private val paint = Paint().apply {
        isAntiAlias = true
        blendMode = params.blendMode
        colorFilter = if (params.color != Color.Unspecified) {
            ColorFilter.tint(params.color, BlendMode.SrcIn)
        } else null
    }

    // Track recent stamps for advanced effects
    private val recentStamps = mutableListOf<StampInfo>()

    private data class StampInfo(
        val position: Offset,
        val timestamp: Long,
        val size: Float,
        val angle: Float
    )

    // --- Core Stamp Function ---

    private fun stampTexture(
        canvas: Canvas,
        position: Offset,
        pressure: Float,
        velocity: Float,
        isInitial: Boolean = false
    ): DirtyRect {
        // Select texture (random if multiple available)
        val selectedTexture = if (textures.size == 1) {
            textures[0]
        } else {
            textures[random.nextInt(textures.size)]
        }

        // Calculate base size
        var stampSize = params.size

        // Apply pressure scaling
        if (config.pressureAffectsSize) {
            stampSize *= pressure.coerceIn(0.1f, 2f)
        }

        // Apply velocity scaling
        if (config.velocityAffectsSize) {
            val velocityFactor = (1f + velocity * 0.0001f).coerceIn(0.5f, 2f)
            stampSize *= velocityFactor
        }

        // Apply random scale variation
        if (config.scaleVariation > 0f) {
            val variation = 1f + (random.nextFloat() - 0.5f) * config.scaleVariation
            stampSize *= variation.coerceIn(0.3f, 3f)
        }

        // Calculate rotation
        val rotation = calculateRotation(position, isInitial)

        // Apply scatter/jitter
        val scatteredPosition = if (config.scatterRadius > 0f) {
            val scatterAngle = random.nextFloat() * 2f * PI.toFloat()
            val scatterDistance = random.nextFloat() * config.scatterRadius
            Offset(
                position.x + cos(scatterAngle) * scatterDistance,
                position.y + sin(scatterAngle) * scatterDistance
            )
        } else position

        // Calculate opacity
        var opacity = params.color.alpha
        if (config.pressureAffectsOpacity) {
            opacity *= pressure.coerceIn(0.1f, 1f)
        }
        if (config.opacityVariation > 0f) {
            val variation = 1f + (random.nextFloat() - 0.5f) * config.opacityVariation
            opacity *= variation.coerceIn(0.1f, 1f)
        }

        // Update paint opacity
        paint.alpha = opacity

        // Calculate flip transformation
        val flipX = if (config.flipRandomly && random.nextBoolean()) -1f else 1f
        val flipY = if (config.flipRandomly && random.nextBoolean()) -1f else 1f

        // Draw the stamp
        val halfSize = stampSize * 0.5f
        canvas.save()

        // Apply transformations
        canvas.translate(scatteredPosition.x, scatteredPosition.y)
        canvas.rotate(rotation)
        canvas.scale(flipX, flipY)

        // Define destination rectangle
        val destRect = Rect(-halfSize, -halfSize, halfSize, halfSize)

        canvas.drawImageRect(
            image = selectedTexture,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(selectedTexture.width, selectedTexture.height),
            dstOffset = IntOffset(destRect.left.toInt(), destRect.top.toInt()),
            dstSize = IntSize(destRect.width.toInt(), destRect.height.toInt()),
            paint = paint
        )

        canvas.restore()

        // Record stamp info for potential future use
        recentStamps.add(
            StampInfo(
                position = scatteredPosition,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                size = stampSize,
                angle = rotation
            )
        )

        // Clean up old stamps (keep only recent ones)
        val currentTime = Clock.System.now().toEpochMilliseconds()
        recentStamps.removeAll { currentTime - it.timestamp > 5000 } // 5 seconds

        // Calculate dirty rectangle with padding for rotation and effects
        val padding = stampSize * 0.6f // Extra padding for rotation and scatter
        return Rect(
            scatteredPosition.x - padding,
            scatteredPosition.y - padding,
            scatteredPosition.x + padding,
            scatteredPosition.y + padding
        )
    }

    private fun calculateRotation(position: Offset, isInitial: Boolean): Float {
        return when (config.rotationMode) {
            RotationMode.NONE -> {
                0f
            }
            RotationMode.STROKE_DIRECTION -> {
                if (isInitial) 0f else strokeDirection + getRotationRandomness()
            }
            RotationMode.FIXED_ANGLE -> {
                fixedAngle + getRotationRandomness()
            }
            RotationMode.RANDOM -> {
                random.nextFloat() * 360f
            }
            RotationMode.FOLLOW_PATH -> {
                // Smooth direction following with some history
                val smoothedDirection = if (recentStamps.isNotEmpty()) {
                    val recentAngles = recentStamps.takeLast(3).map { it.angle }
                    recentAngles.average().toFloat()
                } else strokeDirection
                smoothedDirection + getRotationRandomness()
            }
        }
    }

    private fun getRotationRandomness(): Float {
        return if (config.rotationRandomness > 0f) {
            (random.nextFloat() - 0.5f) * config.rotationRandomness
        } else 0f
    }

    private fun updateStrokeDirection(from: Offset, to: Offset) {
        val dx = to.x - from.x
        val dy = to.y - from.y
        if (dx != 0f || dy != 0f) {
            strokeDirection = toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        }
    }

    // --- StrokeSession Implementation ---

    override fun start(event: GestureEvent): DirtyRect {
        lastPosition = event.position
        currentPosition = event.position
        residualDistance = 0f
        strokeDirection = 0f
        recentStamps.clear()

        val pressure = event.pressure ?: params.pressure
        return stampTexture(canvas, event.position, pressure, 0f, isInitial = true)
    }

    override fun move(event: GestureEvent): DirtyRect {
        val newPosition = event.position
        val pressure = event.pressure ?: params.pressure
        val velocity = event.velocity ?: 0f

        // Update stroke direction
        updateStrokeDirection(currentPosition, newPosition)

        // Calculate distance traveled
        val distance = currentPosition.distanceTo(newPosition)
        val totalDistance = residualDistance + distance

        var dirty: DirtyRect = null

        // Stamp at intervals based on spacing
        if (totalDistance >= spacing && spacing > 0f) {
            val numberOfStamps = (totalDistance / spacing).toInt()
            val actualSpacing = totalDistance / numberOfStamps

            for (i in 0 until numberOfStamps) {
                val t = (residualDistance + actualSpacing * (i + 1)) / (residualDistance + distance)
                val stampPosition = currentPosition.lerp(newPosition, t.coerceIn(0f, 1f))

                val stampDirty = stampTexture(canvas, stampPosition, pressure, velocity)
                dirty = dirty.union(stampDirty)
            }

            residualDistance = totalDistance - (numberOfStamps * spacing)
        } else {
            residualDistance = totalDistance
        }

        lastPosition = currentPosition
        currentPosition = newPosition
        return dirty
    }

    override fun end(event: GestureEvent): DirtyRect {
        // Process final movement
        val moveDirty = move(event)

        // Always place a final stamp at the end position for clean stroke endings
        val pressure = event.pressure ?: params.pressure
        val velocity = event.velocity ?: 0f
        val endStampDirty = stampTexture(canvas, event.position, pressure, velocity)

        return moveDirty.union(endStampDirty)
    }
}

// --- Extension Functions for Easy Brush Creation ---
