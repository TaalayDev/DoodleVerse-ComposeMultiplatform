package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random


/**
 * Utility to generate simple procedural textures for stamps
 */
object ProceduralTextureGenerator {

    /**
     * Soft, round brush stamp. `softness` in [0f..1f] — higher = softer edge.
     */
    fun generateCircularBrushTexture(
        size: Int = 64,
        softness: Float = 0.35f
    ): ImageBitmap = image(size, size) { canvas, paint ->
        clear(canvas, size, size)
        val cx = size * 0.5f
        val cy = size * 0.5f
        val rMax = (min(size, size) * 0.5f) - 1f

        // Draw from outer to inner with increasing alpha to approximate a radial falloff.
        val rings = max(8, (size * 0.6f).toInt())
        for (i in rings downTo 0) {
            val t = i / rings.toFloat() // 0..1 (outer->inner)
            val r = rMax * t
            val falloff = smoothFalloff(1f - t, softness)
            paint.color = Color.White
            paint.alpha = falloff
            paint.isAntiAlias = true
            canvas.drawCircle(Offset(cx, cy), r, paint)
        }

        // Optional very subtle core for a natural look
        paint.alpha = 1f
        canvas.drawCircle(Offset(cx, cy), rMax * 0.1f, paint)
    }

    /**
     * Puffy cloud / airbrush mask. `softness` in [0f..1f] controls how soft puffs are.
     * Internally scatters many soft discs with random sizes and opacities.
     */
    fun generateCloudTexture(
        size: Int = 128,
        softness: Float = 0.65f,
        seed: Int = 1337
    ): ImageBitmap = image(size, size) { canvas, paint ->
        clear(canvas, size, size)
        val rng = Random(seed)
        val center = Offset(size * 0.5f, size * 0.5f)
        val baseR = size * 0.35f

        // Core puffs
        val puffs = (size * 0.45f).toInt().coerceIn(24, 120)
        repeat(puffs) {
            // Bias to center with sqrt distribution
            val a = rng.nextFloat() * (2f * PI.toFloat())
            val r = sqrt(rng.nextFloat()) * baseR * (0.8f + rng.nextFloat() * 0.6f)
            val pos = Offset(center.x + cos(a) * r, center.y + sin(a) * r)

            val radius = (baseR * (0.25f + rng.nextFloat() * 0.6f))
            val rings = (12 + rng.nextInt(12))
            // Draw a soft disc by layering smaller discs
            for (j in rings downTo 0) {
                val t = j / rings.toFloat()
                val rr = radius * t
                val alpha = smoothFalloff(1f - t, softness) * (0.22f + rng.nextFloat() * 0.35f)
                paint.color = Color.White
                paint.alpha = alpha
                paint.isAntiAlias = true
                canvas.drawCircle(pos, rr, paint)
            }
        }

        // Gentle global edge darkening (gives form)
        paint.color = Color.White
        paint.isAntiAlias = true
        val outerR = size * 0.46f
        for (i in 16 downTo 0) {
            val t = i / 16f
            val rr = outerR * t
            paint.alpha = 0.06f * smoothFalloff(1f - t, 0.75f)
            canvas.drawCircle(center, rr, paint)
        }
    }

    /**
     * Star-shaped mask with soft edges. `points` >= 3.
     */
    fun generateStarTexture(
        size: Int = 96,
        points: Int = 5,
        innerRatio: Float = 0.48f
    ): ImageBitmap = image(size, size) { canvas, paint ->
        clear(canvas, size, size)
        val cx = size * 0.5f
        val cy = size * 0.5f
        val rOuter = size * 0.45f
        val rInner = rOuter * innerRatio.coerceIn(0.2f, 0.9f)
        val path = starPath(cx, cy, rOuter, rInner, points.coerceAtLeast(3))

        // Soft edge via scaled fills from slightly smaller to full size
        val steps = 18
        for (i in steps downTo 0) {
            val t = i / steps.toFloat()
            val scale = 0.8f + 0.2f * (1f - t) // 0.8 -> 1.0
            val alpha = 0.08f + 0.92f * (1f - t).pow(1.6f)
            paint.color = Color.White
            paint.alpha = alpha
            canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(scale, scale)
            canvas.translate(-cx, -cy)
            canvas.drawPath(path, paint)
            canvas.restore()
        }
    }

    /**
     * Narrow vertical "hair strand" texture with subtle waviness + frayed tip.
     * Great as a mask for bristle brushes (use it tall and skinny).
     */
    fun generateHairStrandTexture(
        width: Int = 32,
        height: Int = 256,
        fray: Float = 0.25f,     // 0..1, how much the tip frays
        curvature: Float = 0.06f,// 0..0.2 small is natural
        seed: Int = 42
    ): ImageBitmap = image(width, height) { canvas, paint ->
        clear(canvas, width, height)
        val rng = Random(seed)
        val cx = width * 0.5f
        val baseHalf = max(1f, width * 0.16f)

        val strands = max(3, (width / 4).coerceAtMost(12))
        repeat(strands) { k ->
            val offsetX = (rng.nextFloat() - 0.5f) * width * 0.2f
            val wiggle = (rng.nextFloat() - 0.5f) * curvature * 2f
            val thickness = baseHalf * (0.7f + rng.nextFloat() * 0.7f)

            val steps = height
            for (y in 0 until steps) {
                val t = y / (steps - 1f) // 0..1 from root(top) to tip(bottom)
                // Subtle sideways curve
                val curveX = sin(t * PI * (1f + wiggle)) * width * curvature
                // Taper
                val half = thickness * (1f - t).pow(1.4f)
                // Fray near tip
                val frayAmt = if (t > 0.8f) (t - 0.8f) / 0.2f else 0f
                val jitter = (rng.nextFloat() - 0.5f) * fray * frayAmt * width * 0.5f

                val xCenter = cx + offsetX + curveX + jitter
                val alpha = (1f - t).pow(0.9f) * 0.9f
                paint.color = Color.White
                paint.alpha = alpha
                canvas.drawRect(
                    left = ((xCenter - half).toFloat()),
                    top = y.toFloat(),
                    right = ((xCenter + half).toFloat()),
                    bottom = (y + 1).toFloat(),
                    paint = paint
                )
            }
        }
    }

    /**
     * Small white noise / dither mask: random tiny dots; good for extra texture.
     */
    fun generateNoiseDotTexture(
        size: Int = 64,
        density: Float = 0.12f,
        seed: Int = 7
    ): ImageBitmap = image(size, size) { canvas, paint ->
        clear(canvas, size, size)
        val rng = Random(seed)
        val count = (size * size * density.coerceIn(0f, 1f)).toInt()
        paint.color = Color.White
        paint.isAntiAlias = true
        repeat(count) {
            val x = rng.nextFloat() * size
            val y = rng.nextFloat() * size
            val r = 0.5f + rng.nextFloat() * 1.5f
            paint.alpha = 0.3f + rng.nextFloat() * 0.7f
            canvas.drawCircle(Offset(x, y), r, paint)
        }
    }

    // endregion

    // region --- Helpers ---

    private inline fun image(
        width: Int,
        height: Int,
        draw: (canvas: Canvas, paint: Paint) -> Unit
    ): ImageBitmap {
        val img = ImageBitmap(width, height, ImageBitmapConfig.Argb8888)
        val canvas = Canvas(img)
        val paint = Paint()
        draw(canvas, paint)
        return img
    }

    private fun clear(canvas: Canvas, width: Int, height: Int) {
        val p = Paint().apply { alpha = 1f }
        canvas.saveLayer(Rect(0f, 0f, width.toFloat(), height.toFloat()), p)
        // Clear via dstOut trick: draw a full-rect with alpha=0 using BlendMode.Clear
        val clearPaint = Paint().apply { alpha = 1f; blendMode = androidx.compose.ui.graphics.BlendMode.Clear }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), clearPaint)
        canvas.restore()
    }

    private fun smoothFalloff(x: Float, softness: Float): Float {
        // x in [0..1], maps to a smooth S-curve; 'softness' controls edge width
        val s = softness.coerceIn(0.01f, 0.99f)
        val k = lerp(1.8f, 5.0f, s) // higher -> softer
        return x.coerceIn(0f, 1f).pow(k)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun starPath(cx: Float, cy: Float, rOuter: Float, rInner: Float, points: Int): Path {
        val path = Path()
        val step = PI.toFloat() / points
        var angle = -PI.toFloat() / 2f // start at top
        path.moveTo(cx + cos(angle) * rOuter, cy + sin(angle) * rOuter)
        for (i in 1 until points * 2) {
            angle += step
            val r = if (i % 2 == 0) rOuter else rInner
            path.lineTo(cx + cos(angle) * r, cy + sin(angle) * r)
        }
        path.close()
        return path
    }
}