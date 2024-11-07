package io.github.taalaydev.doodleverse.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Destination(val route: String) {
    operator fun invoke(): String = route.substringBefore("?")

    open val args: List<NamedNavArgument> = emptyList()

    data object Home : Destination(route = Routes.HOME)
    data object Drawing : Destination(route = Routes.DRAWING) {
        operator fun invoke(projectId: Long): String {
            return route.appendParams("projectId" to projectId)
        }

        override val args = listOf(
            navArgument("projectId") { type = NavType.LongType }
        )
    }

    object Routes {
        const val HOME = "home"
        const val DRAWING = "drawing/{projectId}"
    }
}

internal fun String.appendParams(vararg params: Pair<String, Any?>): String {
    var result = this
    params.forEach { (key, value) ->
        if (value == null) return@forEach
        result = result.replace("{$key}", value.toString())
    }
    return result
}