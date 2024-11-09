package io.github.taalaydev.doodleverse

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import io.github.taalaydev.doodleverse.database.getRepository
import kotlinx.coroutines.Dispatchers

fun MainViewController() = ComposeUIViewController {
    val platformInfo = remember { IOSPlatform() }

    App(platformInfo)
}