package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.graphics.Path

class PathMeasure {
    private var path: Path? = null
    private var length: Float = 0f

    fun setPath(path: Path, forceClosed: Boolean) {
        this.path = path
        length = 1000f
    }

    val lengthValue: Float
        get() = length

}
