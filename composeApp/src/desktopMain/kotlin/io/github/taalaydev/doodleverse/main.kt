package io.github.taalaydev.doodleverse

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.taalaydev.doodleverse.database.getRepository

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DoodleVerse",
    ) {
        val platformInfo = remember { JVMPlatform() }

        App(platformInfo)
    }
}