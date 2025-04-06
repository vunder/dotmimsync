package com.mimetis.dotmimsync

import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    override fun formatString(format: String, vararg args: Any?): String =
        TODO("Not implemented")
//        NSString.create(format= format, locale = null, arguments = args) ?: ""
}

actual fun getPlatform(): Platform = IOSPlatform()
