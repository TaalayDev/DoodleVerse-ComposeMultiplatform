package io.github.taalaydev.doodleverse.engine.brush.shader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.rotate
import kotlin.math.max
import io.github.taalaydev.doodleverse.engine.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Procedural effects that work on all platforms
 */
sealed class ProceduralEffect {
    abstract fun apply(
        canvas: Canvas,
        center: Offset,
        radius: Float,
        state: ShaderState,
        paint: Paint
    ): Rect

    object Rainbow : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            // Create rainbow gradient effect
            val colors = listOf(
                Color.Red,
                Color(0xFFFFA500), // Orange
                Color.Yellow,
                Color.Green,
                Color.Blue,
                Color(0xFF4B0082), // Indigo
                Color(0xFF8B00FF)  // Violet
            )

            val positions = FloatArray(colors.size) { it.toFloat() / (colors.size - 1) }
            val rotationAngle = (state.time + state.strokeLength * 0.01f) * 60f

            val gradient = SweepGradientShader(
                center = center,
                colors = colors
            )

            paint.shader = gradient
            paint.alpha = state.color.alpha * state.pressure

            canvas.save()
            canvas.rotate(rotationAngle, center.x, center.y)
            canvas.drawCircle(center, radius, paint)
            canvas.restore()

            paint.shader = null
            return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        }
    }

    object PulsatingGlow : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            val pulse = sin(state.time * 3.0).toFloat() * 0.1f + 1.0f
            val maxRadius = radius * pulse

            // Draw multiple glow layers
            val layers = 4
            for (i in layers downTo 1) {
                val layerRadius = maxRadius * (i.toFloat() / layers) * 1.5f
                val layerAlpha = state.color.alpha * state.pressure * (1f - i.toFloat() / layers) * 0.5f

                val gradient = RadialGradientShader(
                    center = center,
                    radius = layerRadius,
                    colors = listOf(
                        state.color.copy(alpha = layerAlpha),
                        state.color.copy(alpha = layerAlpha * 0.3f),
                        Color.Transparent
                    ),
                    colorStops = listOf(0f, 0.6f, 1f)
                )

                paint.shader = gradient
                canvas.drawCircle(center, layerRadius, paint)
                paint.shader = null
            }

            val boundRadius = maxRadius * 2f
            return Rect(center.x - boundRadius, center.y - boundRadius,
                center.x + boundRadius, center.y + boundRadius)
        }
    }

    object NoiseTexture : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            // Generate procedural noise pattern
            val dotCount = (radius * 0.5f).toInt().coerceAtLeast(5)
            paint.color = state.color

            for (i in 0 until dotCount) {
                val angle = (i.toFloat() / dotCount) * 2f * PI.toFloat()
                val r = radius * sqrt(random(state.position.x, state.position.y, i.toFloat()))
                val x = center.x + cos(angle) * r
                val y = center.y + sin(angle) * r

                val dotSize = radius * 0.1f * random(x, y, state.time)
                val alpha = state.color.alpha * state.pressure *
                        (0.3f + 0.7f * random(x + state.strokeLength, y, i.toFloat()))

                paint.alpha = alpha
                canvas.drawCircle(Offset(x, y), dotSize, paint)
            }

            return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        }

        private fun random(a: Float, b: Float, c: Float): Float {
            val value = sin(a * 12.9898f + b * 78.233f + c * 37.719f) * 43758.5453f
            return value - floor(value)
        }
    }

    object ElectricArc : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            // Create electric arc effect with branching
            val arcCount = 3
            val path = Path()

            for (arc in 0 until arcCount) {
                val startAngle = arc * 120f + state.time * 90f
                val arcRadius = radius * (0.6f + arc * 0.2f)

                path.reset()
                path.moveTo(center.x, center.y)

                // Create jagged electric path
                val segments = 8
                for (i in 0..segments) {
                    val t = i.toFloat() / segments
                    val angle = (startAngle + t * 60f) * PI.toFloat() / 180f
                    val jitter = sin(state.time * 10f + i * 2f) * radius * 0.1f
                    val r = arcRadius + jitter

                    val x = center.x + cos(angle) * r
                    val y = center.y + sin(angle) * r

                    if (i == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }

                // Glow effect
                paint.style = PaintingStyle.Stroke
                paint.strokeWidth = 3f
                paint.color = Color.White
                paint.alpha = state.color.alpha * state.pressure * 0.9f
                canvas.drawPath(path, paint)

                paint.strokeWidth = 8f
                paint.color = state.color
                paint.alpha = state.color.alpha * state.pressure * 0.4f
                canvas.drawPath(path, paint)
            }

            paint.style = PaintingStyle.Fill
            val bounds = radius * 1.2f
            return Rect(center.x - bounds, center.y - bounds, center.x + bounds, center.y + bounds)
        }
    }


    /** Blend multiple effects in one stamp */
    data class Combined(private val effects: List<ProceduralEffect>) : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            var bounds = Rect(center.x - 1, center.y - 1, center.x + 1, center.y + 1)
            for (e in effects) {
                bounds = bounds.union(e.apply(canvas, center, radius, state, paint))!!
            }
            return bounds
        }
    }

    /** Ink-like dark core with feathered bleed */
    object InkBleed : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val coreR = radius * 0.65f
            val edgeR = radius * 1.6f

            // Core
            paint.shader = null
            paint.color = state.color.copy(alpha = state.color.alpha * (0.6f + 0.4f * state.pressure))
            canvas.drawCircle(center, coreR, paint)

            // Feathered bleed ring using radial gradient
            val grad = RadialGradientShader(
                center = center,
                radius = edgeR,
                colors = listOf(
                    state.color.copy(alpha = 0.35f * state.pressure),
                    state.color.copy(alpha = 0.10f * state.pressure),
                    Color.Transparent
                ),
                colorStops = listOf(0.75f, 0.92f, 1f)
            )
            paint.shader = grad
            canvas.drawCircle(center, edgeR, paint)
            paint.shader = null

            // Tiny micro-splats
            paint.color = state.color.copy(alpha = 0.12f * state.pressure)
            val dots = (radius * 0.8f).toInt().coerceAtLeast(8)
            repeat(dots) { i ->
                val a = hashS(center.x, center.y, i * 3f) * (2f * kotlin.math.PI.toFloat())
                val r = coreR + hashS(center.y, center.x, i * 7f) * (edgeR - coreR)
                val p = Offset(center.x + kotlin.math.cos(a) * r, center.y + kotlin.math.sin(a) * r)
                canvas.drawCircle(p, radius * 0.06f * (0.5f + hashS(p.x, p.y, state.time)), paint)
            }

            val rMax = edgeR + radius * 0.15f
            return Rect(center.x - rMax, center.y - rMax, center.x + rMax, center.y + rMax)
        }
    }

    /** Soft watercolor blossom that breathes with time & pressure */
    object WatercolorBloom : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val pulsate = (kotlin.math.sin(state.time * 2.0) * 0.07 + 1.0).toFloat()
            val outer = radius * 1.8f * pulsate
            val layers = 5
            for (i in 0 until layers) {
                val t = i / (layers - 1f)
                val r = lerp(radius * 0.6f, outer, t)
                val a = (1f - t) * 0.25f * state.pressure
                val grad = RadialGradientShader(
                    center = center.plus(noiseOffset(center, i, radius * 0.05f)),
                    radius = r,
                    colors = listOf(
                        state.color.copy(alpha = a * 0.7f),
                        state.color.copy(alpha = a * 0.25f),
                        Color.Transparent
                    ),
                    colorStops = listOf(0f, 0.7f, 1f)
                )
                paint.shader = grad
                canvas.drawCircle(center, r, paint)
            }
            paint.shader = null
            val m = outer + radius * 0.2f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Grainy chalk/charcoal speckle */
    object ChalkGrain : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val count = (radius * 2f).toInt().coerceAtLeast(16)
            val baseAlpha = 0.22f * state.pressure
            for (i in 0 until count) {
                val rx = (hashS(center.x, i.toFloat(), state.time) - 0.5f) * radius * 2f
                val ry = (hashS(center.y, i * 4f, state.time) - 0.5f) * radius * 2f
                val p = Offset(center.x + rx, center.y + ry)
                val d = distance(center, p) / radius
                if (d <= 1f) {
                    paint.shader = null
                    paint.color = state.color.copy(alpha = baseAlpha * (1f - d) * (0.6f + 0.4f * hashS(p.x, p.y, i.toFloat())))
                    canvas.drawCircle(p, radius * 0.06f * (0.6f + 0.8f * hashS(p.y, p.x, i * 2f)), paint)
                }
            }
            return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        }
    }

    /** Star/sparkle glints with a soft glow */
    object Sparkle : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val count = 3 + ((radius / 12f).toInt()).coerceAtMost(6)
            val glowR = radius * 1.2f
            // glow
            val g = RadialGradientShader(
                center = center,
                radius = glowR,
                colors = listOf(
                    Color.White.copy(alpha = 0.25f * state.pressure),
                    Color.Transparent
                ),
                colorStops = listOf(0f, 1f)
            )
            paint.shader = g
            canvas.drawCircle(center, glowR, paint)
            paint.shader = null

            // stars
            repeat(count) { i ->
                val angle = (i / count.toFloat() + 0.15f * hashS(center.x, center.y, i.toFloat())) * 360f + state.time * 45f
                drawStar(canvas, center, radius * (0.25f + 0.2f * hashS(i.toFloat(), center.x, state.time)), angle) {
                    paint.style = PaintingStyle.Stroke
                    paint.strokeWidth = 2f
                    paint.color = Color.White.copy(alpha = 0.8f * state.pressure)
                    canvas.drawPath(it, paint)
                    paint.style = PaintingStyle.Fill
                    paint.color = state.color.copy(alpha = 0.5f * state.pressure)
                    canvas.drawPath(it, paint)
                }
            }
            val r = glowR + radius * 0.2f
            return Rect(center.x - r, center.y - r, center.x + r, center.y + r)
        }
    }

    /** Metallic sweep highlight (chrome-ish) */
    object MetallicSheen : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val sweep = SweepGradientShader(
                center = center,
                colors = listOf(
                    Color(0xFF0B0B0B),
                    Color(0xFFAAAAAA),
                    Color(0xFFEFEFEF),
                    Color(0xFF666666),
                    Color(0xFF0B0B0B)
                )
            )
            paint.shader = sweep
            paint.alpha = state.color.alpha * (0.4f + 0.6f * state.pressure)
            canvas.save()
            canvas.rotate((state.time * 80f + state.strokeLength * 0.03f) % 360f, center.x, center.y)
            canvas.drawCircle(center, radius, paint)
            canvas.restore()
            paint.shader = null
            return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        }
    }

    /** Nebula: layered color clouds + twinkles */
    object Nebula : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val cols = listOf(
                state.color,
                state.color.copy(alpha = state.color.alpha * 0.7f),
                Color(0xFF8B00FF).copy(alpha = 0.35f * state.pressure),
                Color(0xFF00D1FF).copy(alpha = 0.25f * state.pressure)
            )
            var maxR = 0f
            for (i in 0 until 4) {
                val r = radius * (0.8f + 0.8f * hashS(center.x, center.y, i * 11f))
                val c = center.plus(noiseOffset(center, i, radius * 0.35f))
                val grad = RadialGradientShader(
                    center = c,
                    radius = r,
                    colors = listOf(cols[i].copy(alpha = cols[i].alpha * 0.7f), Color.Transparent),
                    colorStops = listOf(0f, 1f)
                )
                paint.shader = grad
                canvas.drawCircle(c, r, paint)
                maxR = maxOf(maxR, r)
            }
            paint.shader = null

            // twinkles
            val tw = (radius / 5f).toInt().coerceAtLeast(3)
            repeat(tw) { i ->
                val off = noiseOffset(center, i + 99, radius)
                val p = center + off * 0.6f
                canvas.drawCircle(p, radius * 0.05f, Paint().apply {
                    color = Color.White.copy(alpha = 0.25f * state.pressure)
                })
            }
            val r = maxR + radius * 0.4f
            return Rect(center.x - r, center.y - r, center.x + r, center.y + r)
        }
    }

    /** Fire/ember core with sparks */
    object FireEmber : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val grad = RadialGradientShader(
                center = center,
                radius = radius * 1.2f,
                colors = listOf(
                    Color.White.copy(alpha = 0.9f * state.pressure),
                    Color(0xFFFFF176).copy(alpha = 0.8f * state.pressure),
                    Color(0xFFFF8A65).copy(alpha = 0.6f * state.pressure),
                    Color(0xFFD84315).copy(alpha = 0.0f)
                ),
                colorStops = listOf(0f, 0.35f, 0.7f, 1f)
            )
            paint.shader = grad
            canvas.drawCircle(center, radius * 1.2f, paint)
            paint.shader = null

            // sparks
            val count = (radius).toInt().coerceAtLeast(6)
            repeat(count) { i ->
                val a = (i / count.toFloat()) * 360f + state.time * 160f
                val len = radius * (0.5f + 0.5f * hashS(center.x, center.y, i * 3f))
                val p = Offset(center.x + kotlin.math.cos(a.toRad()) * len, center.y + kotlin.math.sin(a.toRad()) * len)
                paint.color = Color(0xFFFFE082).copy(alpha = 0.6f * state.pressure * hashS(p.x, p.y, state.time))
                canvas.drawCircle(p, radius * 0.06f, paint)
            }
            val m = radius * 1.5f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Glassy bubble with rim light */
    object GlassBubble : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            // body
            val body = RadialGradientShader(
                center = center,
                radius = radius,
                colors = listOf(
                    Color.White.copy(alpha = 0.10f * state.pressure),
                    Color.Transparent
                ),
                colorStops = listOf(0f, 1f)
            )
            paint.shader = body
            canvas.drawCircle(center, radius, paint)

            // rim
            paint.shader = null
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = max(1f, radius * 0.06f)
            paint.color = Color.White.copy(alpha = 0.55f * state.pressure)
            canvas.drawCircle(center, radius * 0.98f, paint)
            paint.style = PaintingStyle.Fill

            // specular dot
            val spec = center + Offset(-radius * 0.35f, -radius * 0.35f)
            paint.color = Color.White.copy(alpha = 0.45f * state.pressure)
            canvas.drawCircle(spec, radius * 0.18f, paint)

            val m = radius * 1.1f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Halftone dot stamp (comic style) */
    object Halftone : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val step = max(3f, radius * 0.22f)
            val r2 = radius * 1.2f
            var y = center.y - r2
            while (y <= center.y + r2) {
                var x = center.x - r2
                while (x <= center.x + r2) {
                    val p = Offset(x, y)
                    val d = distance(center, p) / r2
                    if (d <= 1f) {
                        val dotR = step * 0.35f * (1f - d)
                        paint.color = state.color.copy(alpha = (0.18f + 0.35f * (1f - d)) * state.pressure)
                        canvas.drawCircle(p, dotR, paint)
                    }
                    x += step
                }
                y += step
            }
            val m = r2 + step
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Pixel mosaic tile stamp */
    object PixelMosaic : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val tile = max(2f, radius * 0.18f)
            val half = radius
            var y = center.y - half
            while (y <= center.y + half) {
                var x = center.x - half
                while (x <= center.x + half) {
                    val p = Offset(x + tile * 0.5f, y + tile * 0.5f)
                    val d = distance(center, p) / half
                    if (d <= 1f) {
                        val jitter = (hashS(x, y, state.time) - 0.5f) * 0.08f
                        paint.shader = null
                        paint.color = state.color.copy(alpha = (0.28f + 0.5f * (1f - d)) * state.pressure)
                        canvas.save()
                        canvas.translate(jitter * tile, jitter * tile)
                        canvas.drawRect(Rect(x, y, x + tile, y + tile), paint)
                        canvas.restore()
                    }
                    x += tile
                }
                y += tile
            }
            val m = half + tile
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    object WetInk : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            val outerRadius = radius * 1.5f
            val inkColor = state.color.copy(alpha = state.color.alpha * state.pressure)
            val edgeColor = inkColor.copy(alpha = inkColor.alpha * 0.1f)

            val gradient = RadialGradientShader(
                center = center,
                radius = outerRadius,
                colors = listOf(inkColor, edgeColor, Color.Transparent),
                colorStops = listOf(0f, 0.6f, 1f)
            )

            paint.shader = gradient
            canvas.drawCircle(center, outerRadius, paint)
            paint.shader = null

            return Rect(center.x - outerRadius, center.y - outerRadius, center.x + outerRadius, center.y + outerRadius)
        }
    }

    object Crayon : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            val density = (radius * 2f).toInt().coerceIn(10, 50)
            paint.color = state.color
            paint.style = PaintingStyle.Fill

            for (i in 0 until density) {
                val angle = random(center.x, center.y, i.toFloat()) * 2f * PI.toFloat()
                val r = radius * sqrt(random(center.y, center.x, (i * 2).toFloat()))
                val x = center.x + cos(angle) * r
                val y = center.y + sin(angle) * r

                val dotRadius = radius * 0.4f * random(x, y, state.time)
                val alpha = state.color.alpha * state.pressure * (0.1f + 0.5f * random(y, x, state.strokeLength))

                paint.alpha = alpha.coerceIn(0f, 1f)
                canvas.drawCircle(Offset(x, y), dotRadius, paint)
            }

            return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        }

        private fun random(a: Float, b: Float, c: Float): Float {
            val value = sin(a * 12.9898f + b * 78.233f + c * 37.719f) * 43758.5453f
            return value - floor(value)
        }
    }

    object DashedLine : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            val dashLength = state.size * 2f
            val gapLength = state.size * 1.5f
            val totalSegmentLength = dashLength + gapLength

            if ((state.strokeLength % totalSegmentLength) < dashLength) {
                paint.color = state.color
                paint.alpha = state.color.alpha * state.pressure
                canvas.drawCircle(center, radius, paint)
            }

            return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        }
    }

    object Comet : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            val glowRadius = radius * (2f + state.velocity * 0.05f).coerceAtMost(5f)

            val gradient = RadialGradientShader(
                center = center,
                radius = glowRadius,
                colors = listOf(
                    Color.White,
                    state.color.copy(alpha = state.color.alpha * 0.8f),
                    state.color.copy(alpha = 0f)
                ),
                colorStops = listOf(0.0f, 0.3f, 1.0f)
            )

            paint.shader = gradient
            paint.alpha = state.pressure
            canvas.drawCircle(center, glowRadius, paint)
            paint.shader = null

            return Rect(center.x - glowRadius, center.y - glowRadius, center.x + glowRadius, center.y + glowRadius)
        }
    }

    object Ribbon : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            val numRibbons = 3
            val baseRadius = radius * 0.8f
            val offsetAmount = radius * 0.5f

            val baseColor = state.color
            val colors = listOf(
                baseColor.copy(alpha = 0.5f * baseColor.alpha),
                baseColor.copy(alpha = 0.8f * baseColor.alpha),
                baseColor
            ).reversed()


            canvas.save()
            for (i in 0 until numRibbons) {
                val t = i.toFloat() / (numRibbons - 1)
                val currentRadius = baseRadius * (1f - t * 0.5f)
                paint.color = colors[i]
                paint.alpha = colors[i].alpha * state.pressure

                val xOffset = sin(state.strokeLength * 0.2f + t * PI.toFloat()) * offsetAmount * t
                val yOffset = cos(state.strokeLength * 0.2f + t * PI.toFloat()) * offsetAmount * t

                canvas.drawCircle(center.plus(Offset(xOffset, yOffset)), currentRadius, paint)
            }

            canvas.restore()

            val bounds = radius + offsetAmount
            return Rect(center.x - bounds, center.y - bounds, center.x + bounds, center.y + bounds)
        }
    }

    object Sketchy : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            val lineCount = (state.pressure * 10).toInt().coerceIn(3, 15)
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = state.size * 0.1f * state.pressure

            for (i in 0 until lineCount) {
                val angle = random(center.x, center.y, i.toFloat()) * 2f * PI.toFloat()
                val lineRadius = radius * (0.5f + random(center.y, center.x, i.toFloat()) * 0.5f)

                val startX = center.x + cos(angle) * lineRadius
                val startY = center.y + sin(angle) * lineRadius
                val endX = center.x - cos(angle) * lineRadius
                val endY = center.y - sin(angle) * lineRadius

                paint.color = state.color
                paint.alpha = state.color.alpha * state.pressure * (0.2f + 0.8f * random(startX, startY, state.time))

                canvas.drawLine(Offset(startX, startY), Offset(endX, endY), paint)
            }
            paint.style = PaintingStyle.Fill
            return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        }

        private fun random(a: Float, b: Float, c: Float): Float {
            val value = sin(a * 12.9898f + b * 78.233f + c * 37.719f) * 43758.5453f
            return value - floor(value)
        }
    }

    object Bubbles : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            val bubbleRadius = radius * (0.8f + sin(state.time * 5f + state.strokeLength * 0.1f) * 0.2f)
            val bubbleColor = state.color.copy(alpha = state.color.alpha * state.pressure * 0.3f)
            val edgeColor = state.color.copy(alpha = state.color.alpha * state.pressure * 0.8f)

            val gradient = RadialGradientShader(
                center = center,
                radius = bubbleRadius,
                colors = listOf(bubbleColor, edgeColor),
                colorStops = listOf(0.7f, 1.0f)
            )

            paint.shader = gradient
            canvas.drawCircle(center, bubbleRadius, paint)
            paint.shader = null

            // Highlight
            paint.color = Color.White.copy(alpha = 0.7f * state.pressure)
            val highlightCenter = center.plus(Offset(-bubbleRadius * 0.3f, -bubbleRadius * 0.3f))
            canvas.drawCircle(highlightCenter, bubbleRadius * 0.2f, paint)

            val bounds = bubbleRadius
            return Rect(center.x - bounds, center.y - bounds, center.x + bounds, center.y + bounds)
        }
    }

    object Fire : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            val flameRadius = radius * (1.0f + state.velocity * 0.01f).coerceAtMost(2.5f)
            val yellow = Color(0xFFFFFF00)
            val orange = Color(0xFFFFA500)
            val red = Color(0xFFFF0000)

            val flicker = (1.0f + sin(state.time * 20f + state.position.x * 0.1f) * 0.2f)
            val outerRadius = flameRadius * flicker

            val fireGradient = RadialGradientShader(
                center,
                outerRadius,
                colors = listOf(
                    yellow.copy(alpha = 0.9f * state.pressure),
                    orange.copy(alpha = 0.7f * state.pressure),
                    red.copy(alpha = 0.3f * state.pressure),
                    Color.Transparent
                ),
                colorStops = listOf(0.0f, 0.4f, 0.8f, 1.0f)
            )

            paint.shader = fireGradient
            canvas.drawCircle(center, outerRadius, paint)
            paint.shader = null

            return Rect(center.x - outerRadius, center.y - outerRadius, center.x + outerRadius, center.y + outerRadius)
        }
    }

    /** Iridescent oil shimmer (holographic film feel) */
    object IridescentOil : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            // Rotating sweep for hue-shifted ring
            val sweep = SweepGradientShader(
                center = center,
                colors = listOf(
                    Color(0xFFFF6EC7), // pink
                    Color(0xFF00D1FF), // cyan
                    Color(0xFFFFF176), // yellow
                    Color(0xFFB388FF), // violet
                    Color(0xFFFF6EC7)
                )
            )
            canvas.save()
            canvas.rotate((state.time * 40f + state.strokeLength * 0.05f) % 360f, center.x, center.y)
            paint.shader = sweep
            canvas.drawCircle(center, radius, paint)
            canvas.restore()
            paint.shader = null

            // Soft inner bloom to sell “oil on water”
            val inner = RadialGradientShader(
                center = center,
                radius = radius * 0.9f,
                colors = listOf(
                    Color.White.copy(alpha = 0.08f * state.pressure),
                    Color.Transparent
                ),
                colorStops = listOf(0f, 1f)
            )
            paint.shader = inner
            canvas.drawCircle(center, radius * 0.9f, paint)
            paint.shader = null
            return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        }
    }

    /** Animated aurora ribbons, layered translucent bands */
    data class AuroraRibbon(val bands: Int = 3, val seed: Int = 7) : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val count = bands.coerceIn(2, 6)
            repeat(count) { i ->
                val r = radius * (0.9f + 0.35f * hashS(center.x, seed + i * 3f, center.y))
                val phase = state.time * (0.7f + i * 0.1f) + i * 0.8f
                val c = center + Offset(
                    (hashS(phase, seed * 2f, i.toFloat()) - 0.5f) * radius * 0.6f,
                    (hashS(phase * 1.2f, i.toFloat(), seed.toFloat()) - 0.5f) * radius * 0.6f
                )
                val grad = SweepGradientShader(
                    center = c,
                    colors = listOf(
                        state.color.copy(alpha = 0.18f * state.pressure),
                        Color(0xFF00D1FF).copy(alpha = 0.18f * state.pressure),
                        Color(0xFF8B00FF).copy(alpha = 0.18f * state.pressure),
                        state.color.copy(alpha = 0.18f * state.pressure)
                    )
                )
                paint.shader = grad
                canvas.save()
                canvas.rotate((phase * 30f) % 360f, c.x, c.y)
                canvas.drawCircle(c, r, paint)
                canvas.restore()
            }
            paint.shader = null
            val m = radius * 1.4f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Concentric ink ripples expanding with time */
    object RippleInk : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val rings = 4
            for (i in 0 until rings) {
                val t = (i + (state.time * 0.6f % 1f))
                val rr = radius * (0.6f + t)
                paint.style = PaintingStyle.Stroke
                paint.strokeWidth = (radius * 0.08f) * (1f - t)
                paint.color = state.color.copy(alpha = 0.25f * (1f - t) * state.pressure)
                canvas.drawCircle(center, rr, paint)
            }
            paint.style = PaintingStyle.Fill
            val m = radius * 2f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** High-density shiny specks; “cosmic glitter” */
    data class GlitterDust(val density: Float = 1f, val seed: Int = 13) : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val count = (radius * 6f * density).toInt().coerceAtLeast(20)
            repeat(count) { i ->
                val a = hashS(center.x, seed + i * 2f, center.y) * (2f * PI.toFloat())
                val r = hashS(center.y, i * 7f, seed.toFloat()) * radius
                val p = Offset(center.x + kotlin.math.cos(a) * r, center.y + kotlin.math.sin(a) * r)
                val sz = radius * (0.01f + 0.05f * hashS(p.x, p.y, i.toFloat()))
                paint.color = Color.White.copy(alpha = 0.25f * state.pressure * (0.4f + 0.6f * hashS(i.toFloat(), p.x, p.y)))
                canvas.drawCircle(p, sz, paint)
            }
            val m = radius + radius * 0.2f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Soft bokeh disks, useful for dreamy glow trails */
    object BokehLights : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val n = (3 + (radius / 12f).toInt()).coerceAtMost(10)
            repeat(n) { i ->
                val off = noiseOffset(center, 100 + i, radius * (0.4f + 0.4f * hashS(i.toFloat(), center.x, center.y)))
                val p = center + off
                val r0 = radius * (0.18f + 0.22f * hashS(p.x, p.y, i.toFloat()))
                val g = RadialGradientShader(
                    center = p,
                    radius = r0,
                    colors = listOf(
                        state.color.copy(alpha = 0.25f * state.pressure),
                        Color.Transparent
                    ),
                    colorStops = listOf(0f, 1f)
                )
                paint.shader = g
                canvas.drawCircle(p, r0, paint)
            }
            paint.shader = null
            val m = radius * 1.6f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Wispy smoke/ink ovals */
    object SmokeWisps : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val w = 5
            repeat(w) { i ->
                val ang = (i * 72f + state.time * 25f) % 360f
                val p = center + noiseOffset(center, i + 5, radius * 0.45f)
                canvas.save()
                canvas.rotate(ang, p.x, p.y)
                val rr = Rect(p.x - radius * 0.5f, p.y - radius * 0.18f, p.x + radius * 0.5f, p.y + radius * 0.18f)
                val g = RadialGradientShader(
                    center = p,
                    radius = radius * 0.6f,
                    colors = listOf(state.color.copy(alpha = 0.12f * state.pressure), Color.Transparent),
                    colorStops = listOf(0f, 1f)
                )
                paint.shader = g
                canvas.drawOval(rr, paint)
                canvas.restore()
            }
            paint.shader = null
            val m = radius * 1.2f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Porcelain crackle: radial fractures */
    object CrackedPorcelain : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val cracks = (6 + (radius / 10f).toInt()).coerceAtMost(22)
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = max(1f, radius * 0.035f)
            repeat(cracks) { i ->
                val a = (360f / cracks) * i + (hashS(center.x, i * 3f, center.y) - 0.5f) * 25f
                val len = radius * (0.5f + 0.5f * hashS(i.toFloat(), center.x, center.y))
                val p2 = Offset(center.x + kotlin.math.cos(a.toRad()) * len, center.y + kotlin.math.sin(a.toRad()) * len)
                paint.color = Color.Black.copy(alpha = 0.18f * state.pressure)
                canvas.drawLine(center, p2, paint)
            }
            paint.style = PaintingStyle.Fill
            return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        }
    }

    /** Neon donut: hot core + outer glow */
    object NeonRing : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val inner = RadialGradientShader(
                center = center,
                radius = radius * 0.65f,
                colors = listOf(Color.White.copy(alpha = 0.8f * state.pressure), Color.Transparent),
                colorStops = listOf(0f, 1f)
            )
            paint.shader = inner
            canvas.drawCircle(center, radius * 0.65f, paint)

            paint.shader = null
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = radius * 0.22f
            paint.color = state.color.copy(alpha = 0.7f * state.pressure)
            canvas.drawCircle(center, radius * 0.75f, paint)
            paint.style = PaintingStyle.Fill

            val glow = RadialGradientShader(
                center = center, radius = radius * 1.5f,
                colors = listOf(state.color.copy(alpha = 0.22f * state.pressure), Color.Transparent),
                colorStops = listOf(0f, 1f)
            )
            paint.shader = glow
            canvas.drawCircle(center, radius * 1.5f, paint)
            paint.shader = null

            val m = radius * 1.6f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Marble veins: soft sinuous lines */
    object MarbleVeins : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val lines = 5
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = max(1f, radius * 0.04f)
            repeat(lines) { i ->
                val path = Path()
                val steps = 12
                for (k in 0..steps) {
                    val t = k / steps.toFloat()
                    val theta = t * (2f * PI.toFloat())
                    val rr = radius * (0.3f + 0.55f * t) + noiseOffset(center, i * 17 + k, radius * 0.2f).x
                    val p = Offset(center.x + kotlin.math.cos(theta) * rr, center.y + kotlin.math.sin(theta) * rr)
                    if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                paint.color = state.color.copy(alpha = 0.18f * state.pressure)
                canvas.drawPath(path, paint)
            }
            paint.style = PaintingStyle.Fill
            val m = radius * 1.1f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Rainbow prism shards */
    object RainbowPrism : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val shards = 6
            repeat(shards) { i ->
                val ang = (i * (360f / shards) + state.time * 20f) % 360f
                val r = radius * (0.6f + 0.3f * hashS(i.toFloat(), center.x, center.y))
                val p2 = Offset(center.x + kotlin.math.cos(ang.toRad()) * r, center.y + kotlin.math.sin(ang.toRad()) * r)
                val tri = Path().apply {
                    moveTo(center.x, center.y)
                    val side = 20f + radius * 0.12f
                    val pA = p2 + Offset(-side, -side * 0.6f)
                    val pB = p2 + Offset(side, -side * 0.6f)
                    val pC = p2 + Offset(0f, side)
                    lineTo(pA.x, pA.y); lineTo(pB.x, pB.y); lineTo(pC.x, pC.y); close()
                }
                paint.color = when (i % 6) {
                    0 -> Color(0xFFFF1744); 1 -> Color(0xFFFFD600)
                    2 -> Color(0xFF76FF03); 3 -> Color(0xFF00E5FF)
                    4 -> Color(0xFF651FFF); else -> Color(0xFFFF6EC7)
                }.copy(alpha = 0.18f * state.pressure)
                canvas.drawPath(tri, paint)
            }
            val m = radius * 1.3f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Frosted rim + tiny ice crystals */
    object FrostEdge : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val rim = RadialGradientShader(
                center = center, radius = radius,
                colors = listOf(Color(0xFFB3E5FC).copy(alpha = 0.25f * state.pressure), Color.Transparent),
                colorStops = listOf(0.75f, 1f)
            )
            paint.shader = rim
            canvas.drawCircle(center, radius, paint)
            paint.shader = null

            val n = (radius / 4f).toInt().coerceAtLeast(6)
            repeat(n) { i ->
                val a = (360f / n) * i
                val p = Offset(center.x + kotlin.math.cos(a.toRad()) * radius * 0.9f,
                    center.y + kotlin.math.sin(a.toRad()) * radius * 0.9f)
                paint.color = Color.White.copy(alpha = 0.35f * state.pressure)
                canvas.drawCircle(p, radius * 0.05f, paint)
            }
            val m = radius * 1.1f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Tech-y grid with subtle depth */
    object HoloGrid : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val step = max(4f, radius * 0.2f)
            val half = radius * 1.1f
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = max(1f, step * 0.08f)
            paint.color = state.color.copy(alpha = 0.22f * state.pressure)
            var y = center.y - half
            while (y <= center.y + half) {
                canvas.drawLine(Offset(center.x - half, y), Offset(center.x + half, y), paint)
                y += step
            }
            var x = center.x - half
            while (x <= center.x + half) {
                canvas.drawLine(Offset(x, center.y - half), Offset(x, center.y + half), paint)
                x += step
            }
            paint.style = PaintingStyle.Fill
            val m = half
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Spiral galaxy (arms with starlets) */
    object GalaxySpiral : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val arms = 3
            repeat(arms) { arm ->
                val starCount = (radius * 1.2f).toInt().coerceAtLeast(18)
                repeat(starCount) { i ->
                    val t = i / starCount.toFloat()
                    val theta = t * 5.5f * PI.toFloat() + arm * (2f * PI.toFloat() / arms) + state.time * 0.3f
                    val r = radius * (0.2f + t)
                    val p = Offset(center.x + kotlin.math.cos(theta) * r, center.y + kotlin.math.sin(theta) * r)
                    paint.color = Color.White.copy(alpha = (0.12f + 0.3f * (1f - t)) * state.pressure)
                    canvas.drawCircle(p, radius * 0.03f * (0.6f + 0.8f * (1f - t)), paint)
                }
            }
            val m = radius * 1.2f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Lava-lamp blobs: two overlapping soft gradients */
    object LavaLampBlob : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val off = noiseOffset(center, 77, radius * 0.5f)
            val c1 = center + off
            val c2 = center - off
            val r1 = radius * (0.9f + 0.15f * kotlin.math.sin(state.time).toFloat())
            val r2 = radius * (0.8f + 0.15f * kotlin.math.cos(state.time * 0.8).toFloat())

            fun blob(c: Offset, r: Float, a: Float) {
                val g = RadialGradientShader(
                    center = c, radius = r,
                    colors = listOf(state.color.copy(alpha = 0.22f * a * state.pressure), Color.Transparent),
                    colorStops = listOf(0f, 1f)
                )
                paint.shader = g
                canvas.drawCircle(c, r, paint)
            }

            blob(c1, r1, 1f)
            blob(c2, r2, 0.9f)
            paint.shader = null

            val m = radius * 1.5f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    object ChromaticAberration : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            // RGB channel separation effect
            val separation = radius * 0.05f * (1f + state.velocity)
            val baseAlpha = state.color.alpha * state.pressure * 0.7f

            // Red channel
            paint.color = Color.Red
            paint.alpha = baseAlpha
            paint.blendMode = BlendMode.Screen
            canvas.drawCircle(
                Offset(center.x - separation, center.y),
                radius,
                paint
            )

            // Green channel
            paint.color = Color.Green
            paint.alpha = baseAlpha
            canvas.drawCircle(center, radius * 0.95f, paint)

            // Blue channel
            paint.color = Color.Blue
            paint.alpha = baseAlpha
            canvas.drawCircle(
                Offset(center.x + separation, center.y),
                radius,
                paint
            )

            // Core white highlight
            paint.color = Color.White
            paint.alpha = baseAlpha * 0.5f
            paint.blendMode = BlendMode.Plus
            canvas.drawCircle(center, radius * 0.3f, paint)

            paint.blendMode = BlendMode.SrcOver
            val bounds = radius * 1.1f + separation
            return Rect(center.x - bounds, center.y - bounds, center.x + bounds, center.y + bounds)
        }
    }

    object Organic : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            // Organic cell-like structures
            val cells = 7
            val time = state.time * 0.5f

            for (cell in 0 until cells) {
                val cellSeed = cell.toFloat()
                val angle = (cellSeed / cells) * 2f * PI.toFloat()
                val distance = radius * 0.3f * (1f + sin(time + cellSeed))

                val cellCenter = Offset(
                    center.x + cos(angle) * distance,
                    center.y + sin(angle) * distance
                )

                val cellRadius = radius * (0.2f + sin(time * 2f + cellSeed * 2f) * 0.1f)

                // Cell membrane
                val gradient = RadialGradientShader(
                    center = cellCenter,
                    radius = cellRadius,
                    colors = listOf(
                        state.color.copy(alpha = 0.1f),
                        state.color.copy(alpha = 0.4f),
                        state.color.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    colorStops = listOf(0f, 0.5f, 0.8f, 1f)
                )

                paint.shader = gradient
                paint.alpha = state.pressure
                canvas.drawCircle(cellCenter, cellRadius, paint)
                paint.shader = null

                // Cell nucleus
                paint.color = state.color
                paint.alpha = state.color.alpha * state.pressure * 0.6f
                canvas.drawCircle(cellCenter, cellRadius * 0.3f, paint)
            }

            // Connecting tissue
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = 2f
            paint.alpha = state.color.alpha * state.pressure * 0.3f

            for (i in 0 until cells) {
                for (j in i + 1 until cells) {
                    if ((i + j) % 3 == 0) { // Connect some cells
                        val angle1 = (i.toFloat() / cells) * 2f * PI.toFloat()
                        val angle2 = (j.toFloat() / cells) * 2f * PI.toFloat()
                        val d1 = radius * 0.3f * (1f + sin(time + i))
                        val d2 = radius * 0.3f * (1f + sin(time + j))

                        val p1 = Offset(center.x + cos(angle1) * d1, center.y + sin(angle1) * d1)
                        val p2 = Offset(center.x + cos(angle2) * d2, center.y + sin(angle2) * d2)

                        canvas.drawLine(p1, p2, paint)
                    }
                }
            }

            paint.style = PaintingStyle.Fill
            val bounds = radius * 1.3f
            return Rect(center.x - bounds, center.y - bounds, center.x + bounds, center.y + bounds)
        }
    }

    object Crystalline : ProceduralEffect() {
        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            // Geometric crystal facets
            val facets = 6
            val layers = 3

            for (layer in 0 until layers) {
                val layerRadius = radius * (1f - layer * 0.3f)
                val rotation = layer * 30f + state.time * 20f

                val path = Path()
                val points = mutableListOf<Offset>()

                for (i in 0 until facets) {
                    val angle = (i.toFloat() / facets * 360f + rotation) * PI.toFloat() / 180f
                    points.add(Offset(
                        center.x + cos(angle) * layerRadius,
                        center.y + sin(angle) * layerRadius
                    ))
                }

                // Draw facet edges
                path.moveTo(points[0].x, points[0].y)
                for (point in points.drop(1)) {
                    path.lineTo(point.x, point.y)
                }
                path.close()

                // Gradient fill for depth
                val gradient = LinearGradientShader(
                    from = center - Offset(layerRadius, layerRadius),
                    to = center + Offset(layerRadius, layerRadius),
                    colors = listOf(
                        state.color.copy(alpha = 0.3f),
                        state.color.copy(alpha = 0.6f),
                        state.color.copy(alpha = 0.2f)
                    )
                )

                paint.shader = gradient
                paint.alpha = state.pressure * (1f - layer * 0.2f)
                canvas.drawPath(path, paint)
                paint.shader = null

                // Draw connecting lines to center
                paint.style = PaintingStyle.Stroke
                paint.strokeWidth = 1f
                paint.alpha = state.color.alpha * state.pressure * 0.3f
                for (point in points) {
                    canvas.drawLine(center, point, paint)
                }
                paint.style = PaintingStyle.Fill
            }

            return Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
        }
    }

    object Smoke : ProceduralEffect() {
        private fun random(seed: Float): Float {
            val value = sin(seed * 12.9898f) * 43758.5453f
            return value - floor(value)
        }

        override fun apply(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            state: ShaderState,
            paint: Paint
        ): Rect {
            // Wispy smoke effect with turbulence
            val wisps = 8
            val time = state.time * 0.3f

            for (wisp in 0 until wisps) {
                val wispSeed = wisp.toFloat() + state.strokeLength
                val path = Path()

                // Start from center
                path.moveTo(center.x, center.y)

                // Create turbulent upward path
                val segments = 10
                var currentX = center.x
                var currentY = center.y

                for (i in 1..segments) {
                    val t = i.toFloat() / segments

                    // Upward drift with increasing turbulence
                    val drift = sin(time + wispSeed * 2f) * radius * t * 0.5f
                    val rise = -radius * t * 1.5f * (1f + state.velocity * 0.3f)
                    val turbulence = random(wispSeed + i) * radius * t * 0.3f

                    currentX += drift + turbulence * cos(time * 3f + i)
                    currentY += rise + turbulence * sin(time * 3f + i)

                    val controlX = currentX + random(wispSeed + i + 0.5f) * radius * 0.2f
                    val controlY = currentY + random(wispSeed + i + 1.5f) * radius * 0.2f

                    path.quadraticBezierTo(controlX, controlY, currentX, currentY)
                }

                // Fade out based on distance from origin
                val distance = hypot(currentX - center.x, currentY - center.y)
                val fadeFactor = 1f - (distance / (radius * 2f)).coerceIn(0f, 1f)

                paint.style = PaintingStyle.Stroke
                paint.strokeWidth = radius * 0.1f * (1f - wisp.toFloat() / wisps)
                paint.color = state.color
                paint.alpha = state.color.alpha * state.pressure * fadeFactor * 0.3f
                canvas.drawPath(path, paint)
            }

            // Add volumetric glow
            val glowGradient = RadialGradientShader(
                center = center,
                radius = radius * 1.5f,
                colors = listOf(
                    state.color.copy(alpha = 0.2f),
                    state.color.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                colorStops = listOf(0f, 0.6f, 1f)
            )

            paint.style = PaintingStyle.Fill
            paint.shader = glowGradient
            paint.alpha = state.pressure
            canvas.drawCircle(center, radius * 1.5f, paint)
            paint.shader = null

            val bounds = radius * 2f
            return Rect(center.x - bounds, center.y - bounds * 1.5f,
                center.x + bounds, center.y + bounds * 0.5f)
        }
    }

    /** Opaque gouache core with slightly darker, rough edge */
    object GouacheEdge : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            // Core
            paint.shader = null
            paint.style = PaintingStyle.Fill
            paint.color = state.color.copy(alpha = (0.85f * state.pressure).coerceIn(0f, 1f))
            canvas.drawCircle(center, radius * 0.92f, paint)

            // Edge darkening with subtle noise
            val edgeR = radius
            val edge = RadialGradientShader(
                center = center,
                radius = edgeR,
                colors = listOf(
                    state.color.copy(alpha = 0f),
                    state.color.copy(alpha = 0.20f * state.pressure),
                    state.color.copy(alpha = 0.32f * state.pressure)
                ),
                colorStops = listOf(0.78f, 0.92f, 1f)
            )
            paint.shader = edge
            canvas.drawCircle(center, edgeR, paint)
            paint.shader = null

            // Rough edge specks
            paint.color = state.color.copy(alpha = 0.12f * state.pressure)
            val specks = (10 + (radius / 2f).toInt()).coerceAtMost(42)
            repeat(specks) { i ->
                val a = (i / specks.toFloat()) * 360f + (hashS(center.x, i * 3f, center.y) - 0.5f) * 18f
                val rr = radius * (0.86f + 0.12f * hashS(center.y, i * 7f, state.time))
                val p = Offset(center.x + kotlin.math.cos(a.toRad()) * rr, center.y + kotlin.math.sin(a.toRad()) * rr)
                val rDot = radius * 0.045f * (0.5f + hashS(p.x, p.y, i.toFloat()))
                canvas.drawCircle(p, rDot, paint)
            }
            val m = radius * 1.05f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Parallel streaks with broken pigment (dry brush) */
    object DryBrushStreaks : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val streaks = 5
            val length = radius * 1.5f
            val height = radius * 0.22f
            val rot = (hashS(center.x, center.y, state.time) - 0.5f) * 50f
            canvas.save(); canvas.rotate(rot, center.x, center.y)
            repeat(streaks) { i ->
                val y = center.y + (i - streaks/2) * (height * 0.6f)
                val gaps = 7
                repeat(gaps) { g ->
                    val t = g / gaps.toFloat()
                    val gapScale = hashS(i * 9f, g * 5f, state.time)
                    if (gapScale > 0.25f) {
                        val x0 = center.x - length * 0.5f + t * length
                        val seg = length * 0.15f * (0.4f + gapScale)
                        paint.shader = null
                        paint.color = state.color.copy(alpha = 0.28f * state.pressure * (0.6f + 0.4f * gapScale))
                        canvas.drawRect(Rect(x0, y - height * 0.5f, x0 + seg, y + height * 0.5f), paint)
                    }
                }
            }
            canvas.restore()
            val m = radius * 1.7f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Cross‑hatch lines for sketch shading */
    object CrossHatch : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            fun hatch(angle: Float) {
                val step = max(4f, radius * 0.18f)
                val half = radius * 1.2f
                canvas.save(); canvas.rotate(angle, center.x, center.y)
                var x = center.x - half
                paint.style = PaintingStyle.Stroke
                paint.strokeWidth = max(1f, radius * 0.035f)
                paint.color = state.color.copy(alpha = 0.18f * state.pressure)
                while (x <= center.x + half) {
                    val y0 = center.y - half
                    val y1 = center.y + half
                    // skip if outside circle (cheap test sampling mid)
                    val mid = Offset(x, center.y)
                    if (distance(mid, center) <= radius * 1.1f) {
                        canvas.drawLine(Offset(x, y0), Offset(x, y1), paint)
                    }
                    x += step
                }
                canvas.restore()
                paint.style = PaintingStyle.Fill
            }
            hatch(30f); hatch(120f)
            val m = radius * 1.25f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Dense stipple used as soft shadowing */
    object StippleShade : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val count = (radius * 8f).toInt().coerceAtLeast(40)
            repeat(count) { i ->
                val a = hashS(center.x, i * 3f, center.y) * (2f * PI.toFloat())
                val r = radius * kotlin.math.sqrt(hashS(center.y, i * 7f, state.time)) // more near center
                val p = Offset(center.x + kotlin.math.cos(a) * r, center.y + kotlin.math.sin(a) * r)
                val d = distance(center, p) / radius
                paint.color = state.color.copy(alpha = (0.22f * (1f - d)) * state.pressure)
                canvas.drawCircle(p, radius * 0.02f * (0.5f + hashS(p.x, p.y, i.toFloat())), paint)
            }
            val m = radius
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Splattered ink burst with droplets and streaks */
    object InkSplatterBurst : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            // core
            paint.shader = null
            paint.color = state.color.copy(alpha = 0.55f * state.pressure)
            canvas.drawCircle(center, radius * 0.5f, paint)

            val drops = (12 + radius.toInt()).coerceAtMost(42)
            repeat(drops) { i ->
                val a = (i / drops.toFloat()) * 360f + hashS(center.x, i * 11f, center.y) * 40f
                val len = radius * (0.5f + hashS(center.y, i * 5f, state.time))
                val p = Offset(center.x + kotlin.math.cos(a.toRad()) * len, center.y + kotlin.math.sin(a.toRad()) * len)
                val r0 = radius * (0.05f + 0.06f * hashS(p.x, p.y, i.toFloat()))
                paint.color = state.color.copy(alpha = 0.28f * state.pressure)
                canvas.drawCircle(p, r0, paint)

                // occasional streak
                if (hashS(i * 3f, p.x, p.y) > 0.7f) {
                    paint.style = PaintingStyle.Stroke
                    paint.strokeWidth = max(1f, r0 * 0.7f)
                    val p2 = Offset(
                        p.x + kotlin.math.cos(a.toRad()) * r0 * (2f + 4f * hashS(p.x, p.y, state.time)),
                        p.y + kotlin.math.sin(a.toRad()) * r0 * (2f + 4f * hashS(p.y, p.x, state.time))
                    )
                    canvas.drawLine(p, p2, paint)
                    paint.style = PaintingStyle.Fill
                }
            }
            val m = radius * 1.6f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Swirling vortex using sweep modulation */
    object VortexSwirl : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val sweep = SweepGradientShader(
                center = center,
                colors = listOf(
                    state.color.copy(alpha = 0.0f),
                    state.color.copy(alpha = 0.28f * state.pressure),
                    state.color.copy(alpha = 0.0f)
                )
            )
            canvas.save();
            canvas.rotate((state.time * 90f + state.strokeLength * 0.05f) % 360f, center.x, center.y)
            paint.shader = sweep
            canvas.drawCircle(center, radius, paint)
            canvas.restore()
            paint.shader = null

            // inner spiral arms (thin)
            val arms = 3
            paint.style = PaintingStyle.Stroke
            paint.strokeWidth = max(1f, radius * 0.03f)
            paint.color = state.color.copy(alpha = 0.18f * state.pressure)
            repeat(arms) { k ->
                val path = Path()
                val steps = 22
                for (s in 0..steps) {
                    val t = s / steps.toFloat()
                    val ang = (t * 540f + k * 120f + state.time * 20f).toRad()
                    val r = radius * (0.2f + 0.8f * t)
                    val p = Offset(center.x + kotlin.math.cos(ang) * r, center.y + kotlin.math.sin(ang) * r)
                    if (s == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                canvas.drawPath(path, paint)
            }
            paint.style = PaintingStyle.Fill
            val m = radius * 1.2f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** RGB channel misalignment (graphic glitch) */
    object RGBGlitch : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val off = radius * 0.1f * (0.3f + 0.7f * hashS(center.x, center.y, state.time))
            fun draw(offset: Offset, c: Color, a: Float) {
                paint.shader = null; paint.color = c.copy(alpha = a * state.pressure)
                canvas.save(); canvas.translate(offset.x, offset.y)
                canvas.drawCircle(center, radius * 0.92f, paint)
                canvas.restore()
            }
            draw(Offset(-off, 0f), Color(0xFFFF1744), 0.35f)
            draw(Offset(off, 0f), Color(0xFF00E676), 0.35f)
            draw(Offset(0f, off * 0.6f), Color(0xFF2979FF), 0.35f)

            // original layer
            paint.color = state.color.copy(alpha = 0.55f * state.pressure)
            canvas.drawCircle(center, radius * 0.9f, paint)
            val m = radius * 1.2f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Pencil‑like scribbles made of tiny linelets */
    object GraphiteScribble : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val lines = (radius * 2f).toInt().coerceAtLeast(18)
            paint.style = PaintingStyle.Stroke
            paint.strokeCap = StrokeCap.Round
            paint.strokeWidth = max(1f, radius * 0.025f)
            repeat(lines) { i ->
                val a = hashS(center.x, i * 2f, state.time) * (2f * PI.toFloat())
                val r = radius * kotlin.math.sqrt(hashS(center.y, i * 7f, state.time))
                val p = Offset(center.x + kotlin.math.cos(a) * r, center.y + kotlin.math.sin(a) * r)
                val dir = (hashS(p.x, p.y, i.toFloat()) - 0.5f) * 180f
                val len = radius * (0.12f + 0.18f * hashS(i.toFloat(), p.x, p.y))
                val dx = kotlin.math.cos(dir.toRad()) * len
                val dy = kotlin.math.sin(dir.toRad()) * len
                paint.color = state.color.copy(alpha = 0.22f * state.pressure)
                canvas.drawLine(Offset(p.x - dx * 0.5f, p.y - dy * 0.5f), Offset(p.x + dx * 0.5f, p.y + dy * 0.5f), paint)
            }
            paint.style = PaintingStyle.Fill
            val m = radius
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Scatter leaves (simple teardrop shape) */
    object LeafScatter : ProceduralEffect() {
        private fun leafPath(center: Offset, rot: Float, len: Float, fat: Float): Path {
            val p = Path()
            val a = rot.toRad()
            val dir = Offset(kotlin.math.cos(a), kotlin.math.sin(a))
            val n = Offset(-dir.y, dir.x)
            val tip = center + dir * len
            val baseL = center - dir * (len * 0.3f) + n * (fat * 0.5f)
            val baseR = center - dir * (len * 0.3f) - n * (fat * 0.5f)
            p.moveTo(tip.x, tip.y)
            p.lineTo(baseL.x, baseL.y)
            p.lineTo(baseR.x, baseR.y)
            p.close()
            return p
        }
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val count = (4 + (radius / 6f).toInt()).coerceAtMost(18)
            repeat(count) { i ->
                val ang = hashS(center.x, i * 3f, center.y) * 360f
                val r = radius * (0.2f + 0.8f * hashS(center.y, i * 5f, state.time))
                val p = Offset(center.x + kotlin.math.cos(ang.toRad()) * r, center.y + kotlin.math.sin(ang.toRad()) * r)
                val len = radius * (0.18f + 0.22f * hashS(p.x, p.y, i.toFloat()))
                val fat = len * (0.55f + 0.25f * hashS(i.toFloat(), p.x, p.y))
                paint.shader = null
                paint.color = state.color.copy(alpha = 0.28f * state.pressure)
                canvas.drawPath(leafPath(p, ang + 20f, len, fat), paint)
            }
            val m = radius * 1.2f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Flower‑like petals around center */
    object PetalBloom : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val petals = 6
            repeat(petals) { i ->
                val ang = (i * (360f / petals)) + state.time * 10f
                val r = radius * 0.85f
                val a = ang.toRad()
                val p = center + Offset(kotlin.math.cos(a) * r * 0.2f, kotlin.math.sin(a) * r * 0.2f)
                val rr = Rect(p.x - r * 0.55f, p.y - r * 0.25f, p.x + r * 0.55f, p.y + r * 0.25f)
                canvas.save(); canvas.rotate(ang, p.x, p.y)
                val g = RadialGradientShader(
                    center = p,
                    radius = r * 0.6f,
                    colors = listOf(state.color.copy(alpha = 0.28f * state.pressure), Color.Transparent),
                    colorStops = listOf(0f, 1f)
                )
                paint.shader = g
                canvas.drawOval(rr, paint)
                canvas.restore()
            }
            paint.shader = null
            val core = state.color.copy(alpha = 0.4f * state.pressure)
            paint.color = core
            canvas.drawCircle(center, radius * 0.25f, paint)
            val m = radius * 1.1f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Airbrush / spray paint ring */
    object SprayPaint : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            val dots = (radius * 10f).toInt().coerceAtLeast(60)
            val ring = radius * 0.9f
            repeat(dots) { i ->
                val a = hashS(center.x, i * 2f, state.time) * (2f * PI.toFloat())
                val r = (ring + (hashS(center.y, i * 5f, state.time) - 0.5f) * radius * 0.25f)
                val p = Offset(center.x + kotlin.math.cos(a) * r, center.y + kotlin.math.sin(a) * r)
                paint.color = state.color.copy(alpha = 0.14f * state.pressure * (0.5f + hashS(p.x, p.y, i.toFloat())))
                val sz = radius * 0.015f * (0.5f + hashS(i.toFloat(), p.x, p.y))
                canvas.drawCircle(p, sz, paint)
            }
            val m = radius * 1.2f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

    /** Paper cutout: fill with offset shadow */
    object PaperCutout : ProceduralEffect() {
        override fun apply(canvas: Canvas, center: Offset, radius: Float, state: ShaderState, paint: Paint): Rect {
            // Shadow layer (offset, darker)
            val shadowOff = Offset(radius * 0.12f, radius * 0.12f)
            paint.shader = null
            paint.color = Color(0xFF000000).copy(alpha = 0.15f * state.pressure)
            canvas.save(); canvas.translate(shadowOff.x, shadowOff.y)
            canvas.drawCircle(center, radius * 0.95f, paint)
            canvas.restore()

            // Main colored paper
            paint.color = state.color.copy(alpha = 0.95f * state.pressure)
            canvas.drawCircle(center, radius * 0.95f, paint)

            // Inner bevel (light rim)
            val bevel = RadialGradientShader(
                center = center,
                radius = radius,
                colors = listOf(Color.White.copy(alpha = 0.14f * state.pressure), Color.Transparent),
                colorStops = listOf(0.0f, 1f)
            )
            paint.shader = bevel
            canvas.drawCircle(center, radius * 0.9f, paint)
            paint.shader = null
            val m = radius * 1.2f
            return Rect(center.x - m, center.y - m, center.x + m, center.y + m)
        }
    }

}


// ---- small helpers (still inside ProceduralEffect scope or as file-level private) ----
private fun hashS(a: Float, b: Float, c: Float): Float {
    val v = kotlin.math.sin(a * 12.9898f + b * 78.233f + c * 37.719f) * 43758.5453f
    return v - kotlin.math.floor(v)
}
private fun noiseOffset(o: Offset, seed: Int, scale: Float): Offset =
    Offset((hashS(o.x, seed.toFloat(), o.y) - 0.5f) * scale, (hashS(o.y, seed * 2f, o.x) - 0.5f) * scale)

private fun distance(a: Offset, b: Offset) = kotlin.math.hypot(b.x - a.x, b.y - a.y)
private fun Float.toRad() = (this / 180f) * kotlin.math.PI.toFloat()
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private inline fun drawStar(
    canvas: Canvas,
    center: Offset,
    r: Float,
    angleDeg: Float,
    draw: (Path) -> Unit
) {
    val pts = 5
    val inner = r * 0.45f
    val path = Path()
    for (i in 0 until pts * 2) {
        val rr = if (i % 2 == 0) r else inner
        val ang = (i / (pts * 2f)) * 360f + angleDeg
        val p = Offset(center.x + kotlin.math.cos(ang.toRad()) * rr, center.y + kotlin.math.sin(ang.toRad()) * rr)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    draw(path)
}
