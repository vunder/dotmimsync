package com.mimetis.dotmimsync

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat
import platform.Foundation.stringWithString
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override fun formatString(format: String, vararg args: Any?): String =
        NSString.stringWithFormat(format, args)
}

actual fun getPlatform(): Platform = IOSPlatform()