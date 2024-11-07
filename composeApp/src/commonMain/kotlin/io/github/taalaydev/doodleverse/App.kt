package io.github.taalaydev.doodleverse

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview

import io.github.taalaydev.doodleverse.navigation.MainNavHost
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import io.github.taalaydev.doodleverse.ui.theme.AppTheme

@Composable
@Preview
fun App(platform: Platform) {
    AppTheme {
        val navController = rememberNavController()

        MainNavHost(navController = navController, platform = platform)
    }
}

