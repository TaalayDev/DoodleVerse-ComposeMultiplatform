package io.github.taalaydev.doodleverse.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.core.lessons
import io.github.taalaydev.doodleverse.ui.screens.about.AboutScreen
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawingScreen
import io.github.taalaydev.doodleverse.ui.screens.home.HomeScreen
import io.github.taalaydev.doodleverse.ui.screens.lesson.LessonDetailScreen
import io.github.taalaydev.doodleverse.ui.screens.lesson.LessonsScreen

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
        composable(Destination.Lessons.route) {
            LessonsScreen(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }
        composable(
            Destination.LessonDetail.route,
            Destination.LessonDetail.args
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getLong("lessonId") ?: return@composable
            val lesson = lessons.find { it.id == lessonId } ?: return@composable

            LessonDetailScreen(
                platform = platform,
                lesson = lesson,
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(Destination.About.route) {
            AboutScreen(
                navController = navController,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}