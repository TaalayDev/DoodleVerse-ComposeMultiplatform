package io.github.taalaydev.doodleverse.shared

import android.os.Build
import io.github.taalaydev.doodleverse.shared.storage.DataStorage

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()