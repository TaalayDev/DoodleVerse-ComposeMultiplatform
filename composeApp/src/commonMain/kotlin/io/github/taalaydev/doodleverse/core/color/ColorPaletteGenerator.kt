package io.github.taalaydev.doodleverse.core.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.random.Random

/**
 * Intelligent color palette generator that creates harmonious color schemes
 * based on color theory and can extract palettes from images.
 */
class ColorPaletteGenerator(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    /**
     * Available palette types based on color theory
     */
    enum class PaletteType {
        COMPLEMENTARY,      // Colors opposite on color wheel
        ANALOGOUS,          // Colors adjacent on color wheel
        TRIADIC,            // Three colors equally spaced on color wheel
        TETRADIC,           // Four colors forming a rectangle on color wheel
        MONOCHROMATIC,      // Different shades of the same color
        SPLIT_COMPLEMENTARY // Base color + two colors adjacent to its complement
    }

    /**
     * A color palette with a name and list of colors
     */
    data class Palette(
        val id: String = Random.nextLong().toString(),
        val name: String,
        val colors: List<Color>,
        val type: PaletteType
    )

    /**
     * Generates a color palette based on a base color and palette type
     */
    suspend fun generatePalette(
        baseColor: Color,
        type: PaletteType,
        count: Int = 5
    ): Palette = withContext(dispatcher) {
        val hsl = baseColor.toHSL()
        val colors = when (type) {
            PaletteType.COMPLEMENTARY -> {
                complementaryPalette(hsl, count)
            }
            PaletteType.ANALOGOUS -> {
                analogousPalette(hsl, count)
            }
            PaletteType.TRIADIC -> {
                triadicPalette(hsl, count)
            }
            PaletteType.TETRADIC -> {
                tetradicPalette(hsl, count)
            }
            PaletteType.MONOCHROMATIC -> {
                monochromaticPalette(hsl, count)
            }
            PaletteType.SPLIT_COMPLEMENTARY -> {
                splitComplementaryPalette(hsl, count)
            }
        }

        Palette(
            name = "Generated ${type.name.lowercase().capitalize()} Palette",
            colors = colors,
            type = type
        )
    }

    /**
     * Extracts a color palette from an image using k-means clustering
     */
    suspend fun extractPaletteFromImage(
        image: ImageBitmap,
        count: Int = 5
    ): Palette = withContext(dispatcher) {
        val pixels = image.toPixelMap()
        val width = pixels.width
        val height = pixels.height

        // Sample pixels from the image to reduce computation
        val samplingRate = max(1, (width * height) / 10000)
        val samples = mutableListOf<Color>()

        for (y in 0 until height step samplingRate) {
            for (x in 0 until width step samplingRate) {
                samples.add(pixels[x, y])
            }
        }

        // Use K-means clustering to find dominant colors
        val dominantColors = kMeansClustering(samples, count)

        Palette(
            name = "Image Extracted Palette",
            colors = dominantColors,
            type = PaletteType.COMPLEMENTARY // Default type for extracted palettes
        )
    }

    /**
     * Complementary color palette (base color + its opposite)
     */
    private fun complementaryPalette(baseHsl: FloatArray, count: Int): List<Color> {
        val result = mutableListOf<Color>()

        // Add base color
        result.add(hslToColor(baseHsl))

        // Add complementary color (opposite on the color wheel)
        val complementaryHue = (baseHsl[0] + 180) % 360
        result.add(hslToColor(floatArrayOf(complementaryHue, baseHsl[1], baseHsl[2])))

        // Fill remaining colors with variations
        while (result.size < count) {
            if (result.size % 2 == 0) {
                // Add variation of base color
                val saturation = (baseHsl[1] * (0.5f + Random.nextFloat() * 0.5f)).coerceIn(0f, 1f)
                val lightness = (baseHsl[2] * (0.5f + Random.nextFloat() * 0.5f)).coerceIn(0f, 1f)
                result.add(hslToColor(floatArrayOf(baseHsl[0], saturation, lightness)))
            } else {
                // Add variation of complementary color
                val saturation = (baseHsl[1] * (0.5f + Random.nextFloat() * 0.5f)).coerceIn(0f, 1f)
                val lightness = (baseHsl[2] * (0.5f + Random.nextFloat() * 0.5f)).coerceIn(0f, 1f)
                result.add(hslToColor(floatArrayOf(complementaryHue, saturation, lightness)))
            }
        }

        return result
    }

    /**
     * Analogous color palette (adjacent colors on the color wheel)
     */
    private fun analogousPalette(baseHsl: FloatArray, count: Int): List<Color> {
        val result = mutableListOf<Color>()

        // Determine the hue step
        val hueStep = 30f

        // Start with colors to the "left" of the base color on the color wheel
        val startHue = (baseHsl[0] - hueStep * (count / 2)) % 360

        // Generate colors
        for (i in 0 until count) {
            val hue = (startHue + hueStep * i + 360) % 360
            result.add(hslToColor(floatArrayOf(hue, baseHsl[1], baseHsl[2])))
        }

        return result
    }

    /**
     * Triadic color palette (three colors equally spaced on the color wheel)
     */
    private fun triadicPalette(baseHsl: FloatArray, count: Int): List<Color> {
        val result = mutableListOf<Color>()

        // Add the three main triadic colors
        result.add(hslToColor(baseHsl))

        val secondHue = (baseHsl[0] + 120) % 360
        result.add(hslToColor(floatArrayOf(secondHue, baseHsl[1], baseHsl[2])))

        val thirdHue = (baseHsl[0] + 240) % 360
        result.add(hslToColor(floatArrayOf(thirdHue, baseHsl[1], baseHsl[2])))

        // Fill remaining spots with variations
        var currentIndex = 0
        while (result.size < count) {
            val baseIndex = currentIndex % 3
            val hue = when (baseIndex) {
                0 -> baseHsl[0]
                1 -> secondHue
                else -> thirdHue
            }

            val saturation = (baseHsl[1] * (0.7f + Random.nextFloat() * 0.3f)).coerceIn(0f, 1f)
            val lightness = (baseHsl[2] * (0.7f + Random.nextFloat() * 0.3f)).coerceIn(0f, 1f)

            result.add(hslToColor(floatArrayOf(hue, saturation, lightness)))
            currentIndex++
        }

        return result
    }

    /**
     * Tetradic color palette (four colors forming a rectangle on the color wheel)
     */
    private fun tetradicPalette(baseHsl: FloatArray, count: Int): List<Color> {
        val result = mutableListOf<Color>()

        // Add the four main tetradic colors
        result.add(hslToColor(baseHsl))

        val secondHue = (baseHsl[0] + 90) % 360
        result.add(hslToColor(floatArrayOf(secondHue, baseHsl[1], baseHsl[2])))

        val thirdHue = (baseHsl[0] + 180) % 360
        result.add(hslToColor(floatArrayOf(thirdHue, baseHsl[1], baseHsl[2])))

        val fourthHue = (baseHsl[0] + 270) % 360
        result.add(hslToColor(floatArrayOf(fourthHue, baseHsl[1], baseHsl[2])))

        // Fill remaining spots with variations
        var currentIndex = 0
        while (result.size < count) {
            val baseIndex = currentIndex % 4
            val hue = when (baseIndex) {
                0 -> baseHsl[0]
                1 -> secondHue
                2 -> thirdHue
                else -> fourthHue
            }

            val saturation = (baseHsl[1] * (0.7f + Random.nextFloat() * 0.3f)).coerceIn(0f, 1f)
            val lightness = (baseHsl[2] * (0.7f + Random.nextFloat() * 0.3f)).coerceIn(0f, 1f)

            result.add(hslToColor(floatArrayOf(hue, saturation, lightness)))
            currentIndex++
        }

        return result
    }

    /**
     * Monochromatic color palette (different shades of the same color)
     */
    private fun monochromaticPalette(baseHsl: FloatArray, count: Int): List<Color> {
        val result = mutableListOf<Color>()

        // Create variations with different lightness and saturation values
        for (i in 0 until count) {
            val fraction = i.toFloat() / (count - 1)
            val lightness = 0.2f + fraction * 0.6f // Range from 0.2 to 0.8
            val saturation = baseHsl[1] * (0.7f + (1 - fraction) * 0.3f) // Slightly vary saturation

            result.add(hslToColor(floatArrayOf(baseHsl[0], saturation, lightness)))
        }

        return result
    }

    /**
     * Split complementary color palette
     */
    private fun splitComplementaryPalette(baseHsl: FloatArray, count: Int): List<Color> {
        val result = mutableListOf<Color>()

        // Add base color
        result.add(hslToColor(baseHsl))

        // Find the complement
        val complementHue = (baseHsl[0] + 180) % 360

        // Add colors adjacent to the complement
        val firstSplitHue = (complementHue - 30) % 360
        result.add(hslToColor(floatArrayOf(firstSplitHue, baseHsl[1], baseHsl[2])))

        val secondSplitHue = (complementHue + 30) % 360
        result.add(hslToColor(floatArrayOf(secondSplitHue, baseHsl[1], baseHsl[2])))

        // Fill remaining spots with variations
        var currentIndex = 0
        while (result.size < count) {
            val baseIndex = currentIndex % 3
            val hue = when (baseIndex) {
                0 -> baseHsl[0]
                1 -> firstSplitHue
                else -> secondSplitHue
            }

            val saturation = (baseHsl[1] * (0.7f + Random.nextFloat() * 0.3f)).coerceIn(0f, 1f)
            val lightness = (baseHsl[2] * (0.7f + Random.nextFloat() * 0.3f)).coerceIn(0f, 1f)

            result.add(hslToColor(floatArrayOf(hue, saturation, lightness)))
            currentIndex++
        }

        return result
    }

    /**
     * K-means clustering algorithm to find dominant colors in a list of colors
     */
    private fun kMeansClustering(colors: List<Color>, k: Int): List<Color> {
        if (colors.isEmpty()) return emptyList()
        if (colors.size <= k) return colors

        // Initialize centroids randomly
        val centroids = colors.shuffled().take(k).map { it.toVector() }.toMutableList()
        val clusters = MutableList(k) { mutableListOf<Color>() }

        // Run K-means for a fixed number of iterations
        repeat(10) {
            // Clear previous clusters
            clusters.forEach { it.clear() }

            // Assign each color to nearest centroid
            for (color in colors) {
                val colorVector = color.toVector()
                val closestCentroidIndex = centroids.indices.minByOrNull {
                    distance(colorVector, centroids[it])
                } ?: 0
                clusters[closestCentroidIndex].add(color)
            }

            // Update centroids
            for (i in centroids.indices) {
                if (clusters[i].isNotEmpty()) {
                    centroids[i] = clusters[i].map { it.toVector() }
                        .reduce { acc, vector ->
                            FloatArray(3) { j -> acc[j] + vector[j] }
                        }
                        .map { it / clusters[i].size }
                        .toFloatArray()
                }
            }
        }

        // Return centroid colors
        return centroids.map { vectorToColor(it) }
    }

    /**
     * Calculate Euclidean distance between two color vectors
     */
    private fun distance(a: FloatArray, b: FloatArray): Float {
        return a.zip(b).sumOf { (x, y) -> ((x - y) * (x - y)).toDouble() }.toFloat()
    }

    /**
     * Convert a color to RGB vector
     */
    private fun Color.toVector(): FloatArray {
        return floatArrayOf(red, green, blue)
    }

    /**
     * Convert RGB vector to Color
     */
    private fun vectorToColor(vector: FloatArray): Color {
        return Color(vector[0], vector[1], vector[2])
    }

    /**
     * Convert HSL values to Color
     */
    private fun hslToColor(hsl: FloatArray): Color {
        val h = hsl[0]
        val s = hsl[1]
        val l = hsl[2]

        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs((h / 60) % 2 - 1))
        val m = l - c / 2

        val (r1, g1, b1) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f)
        )
    }

    /**
     * Convert a Color to HSL values
     */
    private fun Color.toHSL(): FloatArray {
        val r = red
        val g = green
        val b = blue

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        var h = when {
            delta == 0f -> 0f
            max == r -> ((g - b) / delta) % 6
            max == g -> (b - r) / delta + 2
            else -> (r - g) / delta + 4
        } * 60

        if (h < 0) h += 360

        val l = (max + min) / 2
        val s = if (delta == 0f) 0f else delta / (1 - abs(2 * l - 1))

        return floatArrayOf(h, s, l)
    }

    companion object {
        private fun abs(value: Float): Float = kotlin.math.abs(value)
    }
}