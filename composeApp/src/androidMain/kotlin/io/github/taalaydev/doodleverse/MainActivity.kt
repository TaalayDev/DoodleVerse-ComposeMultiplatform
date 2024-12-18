package io.github.taalaydev.doodleverse

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import io.github.taalaydev.doodleverse.database.getRepository
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.IO

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.initialize(this)
        FileKit.init(this)

        val platformInfo = object : Platform {
            override val name: String = "Android ${Build.VERSION.SDK_INT}"
            override val isWeb: Boolean = false
            override val isDesktop: Boolean = false
            override val isAndroid: Boolean = true
            override val isIos: Boolean = false
            override val projectRepo: ProjectRepository = getRepository(this@MainActivity)
            override val dispatcherIO = kotlinx.coroutines.Dispatchers.IO

            override fun saveImageBitmap(bitmap: ImageBitmap, filename: String, format: ImageFormat) {
                saveImageBitmap(this@MainActivity, bitmap, filename, format)
            }
            override fun launchUrl(url: String): Boolean {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                return true
            }
        }

        setContent {
            App(platformInfo)
        }
    }
}