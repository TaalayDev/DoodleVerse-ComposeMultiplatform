package io.github.taalaydev.doodleverse.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawingScreen
import io.github.taalaydev.doodleverse.ui.screens.home.HomeScreen

@Composable
fun MainNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Drawing.route
    ) {
        composable(Destination.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Destination.Drawing.route) {
            DrawingScreen(navController = navController)
        }
    }
}