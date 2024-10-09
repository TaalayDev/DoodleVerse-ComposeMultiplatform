package io.github.taalaydev.doodleverse.navigation

sealed class Destination(val route: String) {
    operator fun invoke() = route.substringBefore("?")

    data object Home : Destination(route = Routes.HOME)
    data object Drawing : Destination(route = Routes.DRAWING) {
        operator fun invoke(projectId: Long) = route.appendParams("projectId" to projectId)
    }

    object Routes {
        const val HOME = "home"
        const val DRAWING = "drawing"
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