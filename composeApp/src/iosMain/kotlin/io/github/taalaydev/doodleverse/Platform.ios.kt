package io.github.taalaydev.doodleverse

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()