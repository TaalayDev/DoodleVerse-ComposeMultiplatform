package io.github.taalaydev.doodleverse.engine.util

import kotlinx.datetime.Clock

fun Clock.Companion.currentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

fun currentTimeMillis(): Long {
    return Clock.currentTimeMillis()
}