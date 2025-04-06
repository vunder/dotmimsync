package com.mimetis.dotmimsync

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
    override fun formatString(format: String, vararg args: String?): String =
        String.format(format, args)
}

actual fun getPlatform(): Platform = AndroidPlatform()