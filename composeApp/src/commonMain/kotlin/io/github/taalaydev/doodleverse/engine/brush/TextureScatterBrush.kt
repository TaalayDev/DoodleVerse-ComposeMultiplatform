package io.github.taalaydev.doodleverse.engine.brush

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
 * Texture scatter brush that randomly distributes texture pieces along the stroke path.
 * Perfect for creating organic effects like leaves, snow, stars, or abstract patterns.
 * Each texture piece can have random size, rotation, opacity, and position variations.
 */
class TextureScatterBrush(
    override val texture: ImageBitmap,
    private val additionalTextures: List<ImageBitmap> = emptyList(),
    private val scatterRadius: Float = 15f, // How far from stroke path to scatter
    private val density: Float = 0.8f, // How densely packed the scattered pieces are (0-1)
    private val sizeVariation: Float = 0.4f, // Random size variation (0-1)
    private val rotationVariation: Float = 180f, // Random rotation range in degrees
    private val opacityVariation: Float = 0.3f, // Random opacity variation (0-1)
    private val colorTinting: Boolean = true, // Whether to tint textures with brush color
    private val followStroke: Boolean = false // Whether pieces align with stroke direction
) : TextureBrush() {

    override val id = ToolId("texture_scatter")
    override val name: String = "Texture Scatter"

    private val allTextures = listOf(texture) + additionalTextures

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = TextureScatterStrokeSession(
        canvas, allTextures, params, scatterRadius, density, sizeVariation,
        rotationVariation, opacityVariation, colorTinting, followStroke
    )

    private class TextureScatterStrokeSession(
        private val canvas: Canvas,
        private val textures: List<ImageBitmap>,
        params: BrushParams,
        private val scatterRadius: Float,
        private val density: Float,
        private val sizeVariation: Float,
        private val rotationVariation: Float,
        private val opacityVariation: Float,
        private val colorTinting: Boolean,
        private val followStroke: Boolean
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            blendMode = params.blendMode
        }

        private val stepPx: Float = params.size * 0.4f
        private val path = Path()

        // Random with consistent seed
        private val random: Random = Random(params.hashCode() + Clock.System.now().toEpochMilliseconds().toInt())

        // State tracking
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f
        private var currentStrokeDirection: Float = 0f

        // Track scattered pieces to avoid overlap
        private val scatteredPieces = mutableListOf<ScatteredPiece>()

        private data class ScatteredPiece(
            val position: Offset,
            val size: Float,
            val timestamp: Long = Clock.System.now().toEpochMilliseconds()
        )

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun radiusFor(pressure: Float): Float = max(2f, params.size * pressure * 0.5f)

        /**
         * Generate random position within scatter radius using different distribution patterns
         */
        private fun generateScatterPosition(center: Offset, radius: Float): Offset {
            val angle = random.nextFloat() * 2f * PI.toFloat()

            // Use different distribution patterns for more natural scattering
            val distance = when (random.nextInt(3)) {
                0 -> random.nextFloat() * radius // Uniform distribution
                1 -> radius * sqrt(random.nextFloat()) // More concentrated toward center
                else -> radius * random.nextFloat().pow(0.5f) // Even more centered
            }

            return Offset(
                center.x + cos(angle) * distance,
                center.y + sin(angle) * distance
            )
        }

        /**
         * Check if position is too close to existing pieces
         */
        private fun isTooCloseToExisting(position: Offset, size: Float): Boolean {
            val currentTime = Clock.System.now().toEpochMilliseconds()

            // Remove old pieces (older than 1 second)
            scatteredPieces.removeAll { currentTime - it.timestamp > 1000 }

            return scatteredPieces.any { piece ->
                val distance = piece.position.distanceTo(position)
                distance < (piece.size + size) * 0.7f // Allow some overlap
            }
        }

        private fun drawScatterStamp(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            pressure: Float,
            strokeDirection: Float
        ): DirtyRect {
            val scatterArea = radius * (1f + scatterRadius * 0.1f)
            val adjustedDensity = density * pressure

            // Calculate number of pieces to scatter based on area and density
            val baseCount = (PI * scatterArea * scatterArea * adjustedDensity * 0.01f).toInt()
            val pieceCount = (baseCount * (0.7f + random.nextFloat() * 0.6f)).toInt().coerceIn(1, 20)

            var dirty: DirtyRect = null

            repeat(pieceCount) {
                val scatterPos = generateScatterPosition(center, scatterArea)

                // Select random texture
                val selectedTexture = textures[random.nextInt(textures.size)]

                // Calculate piece size with variation
                val baseSize = params.size * (0.3f + random.nextFloat() * 0.7f) * pressure
                val sizeVar = 1f + (random.nextFloat() - 0.5f) * sizeVariation
                val pieceSize = (baseSize * sizeVar).coerceIn(2f, params.size * 2f)

                // Skip if too close to existing pieces (for some natural spacing)
                if (density < 1f && isTooCloseToExisting(scatterPos, pieceSize)) {
                    return@repeat
                }

                // Calculate rotation
                val rotation = if (followStroke) {
                    strokeDirection + (random.nextFloat() - 0.5f) * rotationVariation * 0.3f
                } else {
                    random.nextFloat() * rotationVariation
                }

                // Calculate opacity
                val baseOpacity = params.color.alpha * (0.5f + pressure * 0.5f)
                val opacityVar = 1f + (random.nextFloat() - 0.5f) * opacityVariation
                val pieceOpacity = (baseOpacity * opacityVar).coerceIn(0.1f, 1f)

                // Create paint for this piece
                val piecePaint = paint.copy().apply {
                    alpha = pieceOpacity
                    if (colorTinting) {
                        colorFilter = ColorFilter.tint(params.color, BlendMode.Modulate)
                    }
                }

                // Draw the scattered texture piece
                canvas.save()
                canvas.translate(scatterPos.x, scatterPos.y)
                canvas.rotate(toDegrees(rotation.toDouble()).toFloat())

                val halfSize = pieceSize * 0.5f
                val destRect = Rect(-halfSize, -halfSize, halfSize, halfSize)

                canvas.drawImageRect(
                    image = selectedTexture,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(selectedTexture.width, selectedTexture.height),
                    dstOffset = IntOffset(destRect.left.toInt(), destRect.top.toInt()),
                    dstSize = IntSize(destRect.width.toInt(), destRect.height.toInt()),
                    paint = piecePaint
                )

                canvas.restore()

                // Track this piece
                scatteredPieces.add(ScatteredPiece(scatterPos, pieceSize))

                // Update dirty rect
                val pieceRect = Rect(
                    scatterPos.x - halfSize - 2f,
                    scatterPos.y - halfSize - 2f,
                    scatterPos.x + halfSize + 2f,
                    scatterPos.y + halfSize + 2f
                )
                dirty = dirty.union(pieceRect)
            }

            return dirty ?: Rect(center.x, center.y, center.x + 1f, center.y + 1f)
        }

        private fun updateStrokeDirection(from: Offset, to: Offset) {
            val dx = to.x - from.x
            val dy = to.y - from.y
            if (dx != 0f || dy != 0f) {
                currentStrokeDirection = atan2(dy, dx)
            }
        }

        private fun qPoint(a: Offset, b: Offset, c: Offset, t: Float): Offset {
            val one = 1f - t
            val x = one * one * a.x + 2f * one * t * b.x + t * t * c.x
            val y = one * one * a.y + 2f * one * t * b.y + t * t * c.y
            return Offset(x, y)
        }

        private fun walkQuadratic(
            canvas: Canvas,
            a: Offset,
            b: Offset,
            c: Offset,
            pStart: Float,
            pEnd: Float
        ): DirtyRect {
            val approxLen = dist(a, c) + 0.5f * (dist(a, b) + dist(b, c) - dist(a, c))
            val subdivisions = max(6, ceil(approxLen / 6f).toInt())
            val dt = 1f / subdivisions

            var dirty: DirtyRect = null
            var prev = a
            var acc = residual

            var t = dt
            while (t <= 1f + 1e-4f) {
                val cur = qPoint(a, b, c, t.coerceAtMost(1f))
                val seg = dist(prev, cur)

                var remain = seg
                while (acc + remain >= stepPx && remain > 0f) {
                    val need = stepPx - acc
                    val f = (need / remain).coerceIn(0f, 1f)
                    val hit = prev.lerp(cur, f)
                    val tt = (t - dt) + dt * f
                    val press = pStart + (pEnd - pStart) * tt

                    updateStrokeDirection(prev, cur)
                    dirty = dirty.union(
                        drawScatterStamp(canvas, hit, radiusFor(press), press, currentStrokeDirection)
                    )
                    prev = hit
                    remain -= need
                    acc = 0f
                }

                acc += remain
                prev = cur
                t += dt
            }

            residual = acc
            return dirty
        }

        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            residual = 0f
            lastPressure = event.pressure ?: params.pressure
            currentStrokeDirection = 0f
            scatteredPieces.clear()

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            return drawScatterStamp(canvas, event.position, radiusFor(lastPressure), lastPressure, 0f)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
            val newPressure = event.pressure ?: params.pressure

            var dirty: DirtyRect = null
            when {
                p0 == null -> {
                    dirty = start(event)
                }
                p1 == null -> {
                    p1 = newP
                    val a = p0!!
                    val m01 = midevent(a, newP)
                    if (lastMid == null) {
                        path.moveTo(a.x, a.y)
                        lastMid = a
                    }
                    path.quadraticBezierTo(a.x, a.y, m01.x, m01.y)
                    dirty = dirty.union(
                        walkQuadratic(
                            canvas = canvas,
                            a = lastMid!!,
                            b = a,
                            c = m01,
                            pStart = lastPressure,
                            pEnd = newPressure
                        )
                    )
                    lastMid = m01
                }
                else -> {
                    val a0 = p0!!
                    val a1 = p1!!
                    val a2 = newP
                    val m1 = midevent(a0, a1)
                    val m2 = midevent(a1, a2)

                    if (lastMid == null) {
                        path.moveTo(m1.x, m1.y)
                        lastMid = m1
                    }

                    path.quadraticBezierTo(a1.x, a1.y, m2.x, m2.y)
                    dirty = dirty.union(
                        walkQuadratic(
                            canvas = canvas,
                            a = lastMid!!,
                            b = a1,
                            c = m2,
                            pStart = lastPressure,
                            pEnd = newPressure
                        )
                    )

                    lastMid = m2
                    p0 = a1
                    p1 = a2
                }
            }

            lastPressure = newPressure
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val endPos = event.position
            val endPressure = event.pressure ?: params.pressure
            val moved = move(event)

            var dirty: DirtyRect = moved
            val a = lastMid
            val ctrl = p1
            if (a != null && ctrl != null) {
                path.quadraticBezierTo(ctrl.x, ctrl.y, ctrl.x, ctrl.y)
                updateStrokeDirection(a, ctrl)
                dirty = dirty.union(
                    walkQuadratic(
                        canvas = canvas,
                        a = a,
                        b = ctrl,
                        c = ctrl,
                        pStart = lastPressure,
                        pEnd = endPressure
                    )
                )
            }

            // Final scatter burst at end
            dirty = dirty.union(
                drawScatterStamp(canvas, endPos, radiusFor(endPressure) * 1.2f, endPressure, currentStrokeDirection)
            )

            return dirty
        }
    }
}