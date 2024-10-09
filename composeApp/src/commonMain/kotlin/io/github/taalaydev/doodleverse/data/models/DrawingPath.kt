package io.github.taalaydev.doodleverse.data.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.random.Random

data class DrawingPath(
    val brush: BrushData,
    val color: Color,
    val size: Float,
    val path: Path = Path(),
    var startPoint: Offset = Offset.Unspecified,
    var endPoint: Offset = Offset.Unspecified,
    private val randoms: MutableMap<String, Float> = HashMap()
) {
    fun getRandom(list: List<Number>): Float {
        val key = list.joinToString()
        return randoms.getOrPut(key) {
            Random.nextFloat()
        }
    }
}