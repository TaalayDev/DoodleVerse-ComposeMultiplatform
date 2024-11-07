package io.github.taalaydev.doodleverse.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform


