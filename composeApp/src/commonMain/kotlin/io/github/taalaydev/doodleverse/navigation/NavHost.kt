package io.github.taalaydev.doodleverse.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawingScreen
import io.github.taalaydev.doodleverse.ui.screens.home.HomeScreen

@Composable
fun MainNavHost(
    navController: NavHostController = rememberNavController(),
    platform: Platform,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                navController = navController,
                platform = platform
            )
        }
        composable(
            Destination.Drawing.route,
            Destination.Drawing.args
        ) {
            val projectId = it.arguments?.getLong("projectId") ?: 0

            DrawingScreen(
                projectId = projectId,
                navController = navController,
                platform = platform
            )
        }
    }
}