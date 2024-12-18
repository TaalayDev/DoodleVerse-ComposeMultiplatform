import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    // Android specific plugins
    alias(libs.plugins.google.playServices)
    alias(libs.plugins.firebase.crashlytics)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }
    
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm("desktop")
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.crashlyticsKtx)

            implementation(project(":database"))
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
            implementation(libs.icons.lucide)
            implementation(libs.colorpicker.compose)
            implementation(libs.material3.window.size.multiplatform)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.datetime)

            implementation(libs.constraintlayout.compose.multiplatform)

            implementation(libs.filekit.compose)

            implementation(libs.reorderable)

            implementation(project(":shared"))
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

            implementation(project(":database"))
        }
        iosMain.dependencies {
            implementation(project(":database"))
        }
    }
}

android {
    namespace = "io.github.taalaydev.doodleverse"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "io.github.taalaydev.doodleverse"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

dependencies {
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.ui.graphics.android)

    implementation("com.google.firebase:firebase-common-ktx:21.0.0")
    implementation("com.google.firebase:firebase-analytics")
}

compose.desktop {
    application {
        mainClass = "io.github.taalaydev.doodleverse.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Pkg)
            packageName = "DoodleVerse"
            packageVersion = "1.0.0"

            macOS {
                bundleID = "io.github.taalaydev.doodleverse"
                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon.icns"))
                minimumSystemVersion = "12.0"

                signing {
                    sign.set(true)
                    identity.set("3rd Party Mac Developer Application: Turgunaliev Daiyrbek (7R25FWCLN3)")
                }

                //provisioningProfile.set(project.file("src/desktopMain/mac/DoodleVerseMacProfile.provisionprofile"))
                //runtimeProvisioningProfile.set(project.file("src/desktopMain/mac/DoodleVerseMacJavaRuntimeProfile.provisionprofile"))

                // entitlementsFile.set(project.file("src/desktopMain/mac/entitlements.plist"))
                // runtimeEntitlementsFile.set(project.file("src/desktopMain/mac/runtime-entitlements.plist"))
            }
            windows {
                // iconFile.set(project.file("icon.ico"))
            }
            linux {
                modules("jdk.security.auth")
                // iconFile.set(project.file("logo.png"))
            }
        }
    }
}
