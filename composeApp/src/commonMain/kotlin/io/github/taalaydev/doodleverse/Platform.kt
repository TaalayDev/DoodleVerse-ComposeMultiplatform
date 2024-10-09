package io.github.taalaydev.doodleverse

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform