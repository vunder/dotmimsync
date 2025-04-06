package com.mimetis.dotmimsync

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override fun formatString(format: String, vararg args: String?): String =
        NSString.stringWithFormat(format, args)
//        NSString.create(format= format, locale = null, arguments = args) ?: ""
}

actual fun getPlatform(): Platform = IOSPlatform()
