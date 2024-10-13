package io.github.taalaydev.doodleverse

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DoodleVerse",
    ) {
        App()
    }
}