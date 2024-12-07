package io.github.taalaydev.doodleverse.shared

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.FirebaseAnalytics
import dev.gitlive.firebase.analytics.analytics

object Analytics {
    private val firebaseAnalytics: FirebaseAnalytics by lazy { Firebase.analytics }

    fun logEvent(name: String, params: Map<String, Any> = emptyMap()) {
        firebaseAnalytics.logEvent(name, params)
    }
}