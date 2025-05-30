package io.github.taalaydev.doodleverse

import android.app.Application

class DoodleVerseApp : Application() {
    companion object {
        lateinit var instance: DoodleVerseApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}