package io.github.taalaydev.doodleverse.engine.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import kotlin.math.max
import kotlin.math.min

fun Paint.copy(): Paint = Paint().also { p ->
    p.isAntiAlias = this.isAntiAlias
    p.style = this.style
    p.strokeWidth = this.strokeWidth
    p.strokeCap = this.strokeCap
    p.blendMode = this.blendMode
    p.color = this.color
    p.colorFilter = this.colorFilter
}


/**
 * Converts an RGB color value to HSL.
 *
 * The conversion formula is adapted from the standard algorithm. It assumes the input
 * Color's r, g, and b components are in the range [0, 1] and returns an array
 * where the hue, saturation, and lightness values are also in the range [0, 1].
 *
 * @param color The androidx.compose.ui.graphics.Color to convert.
 * @return A FloatArray of size 3 containing the H, S, and L values, respectively.
 */
fun rgbToHsl(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue

    val max = max(r, max(g, b))
    val min = min(r, min(g, b))

    var h: Float
    val s: Float
    val l = (max + min) / 2f

    if (max == min) {
        // Achromatic (grayscale)
        h = 0f
        s = 0f
    } else {
        val d = max - min
        s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            b -> (r - g) / d + 4f
            else -> 0f // Should not happen
        }
        h /= 6f
    }

    return floatArrayOf(h, s, l)
}

/**
 * Converts an HSL color value to RGB.
 *
 * The conversion formula is adapted from the standard algorithm. It assumes the input
 * h, s, and l values are in the range [0, 1] and returns a
 * androidx.compose.ui.graphics.Color with r, g, and b values in [0, 1].
 *
 * @param hsl A FloatArray of size 3 containing the H, S, and L values to convert.
 * @return The corresponding androidx.compose.ui.graphics.Color.
 */
fun hslToRgb(hsl: FloatArray): Color {
    val h = hsl[0]
    val s = hsl[1]
    val l = hsl[2]

    val r: Float
    val g: Float
    val b: Float

    if (s == 0f) {
        // Achromatic (grayscale)
        r = l
        g = l
        b = l
    } else {
        fun hueToRgb(p: Float, q: Float, t: Float): Float {
            var tempT = t
            if (tempT < 0f) tempT += 1f
            if (tempT > 1f) tempT -= 1f
            return when {
                tempT < 1f / 6f -> p + (q - p) * 6f * tempT
                tempT < 1f / 2f -> q
                tempT < 2f / 3f -> p + (q - p) * (2f / 3f - tempT) * 6f
                else -> p
            }
        }

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        r = hueToRgb(p, q, h + 1f / 3f)
        g = hueToRgb(p, q, h)
        b = hueToRgb(p, q, h - 1f / 3f)
    }

    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
}