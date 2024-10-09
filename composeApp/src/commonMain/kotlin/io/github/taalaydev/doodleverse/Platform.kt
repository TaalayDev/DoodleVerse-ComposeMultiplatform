package io.github.taalaydev.doodleverse

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform