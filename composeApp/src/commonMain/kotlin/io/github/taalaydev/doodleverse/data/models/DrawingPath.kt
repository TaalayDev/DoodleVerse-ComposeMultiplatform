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
    val points: MutableList<PointModel> = mutableListOf(),
    var startPoint: Offset = Offset.Unspecified,
    var endPoint: Offset = Offset.Unspecified,
    private val randoms: MutableMap<String, Float> = HashMap()
) {
    fun getRandom(list: List<Number>): Float {
        return Random.nextFloat()
    }

    fun randomsString(): String {
        return randoms.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    companion object {
        fun pathFromOffsets(offsets: List<PointModel>): Path {
            val path = Path()
            if (offsets.isNotEmpty()) {
                path.moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach {
                    path.quadraticBezierTo(it.x, it.y, it.x, it.y)
                }
            }
            return path
        }
    }
}

data class PointModel(
    val x: Float,
    val y: Float,
) {
    fun toOffset() = Offset(x, y)
}