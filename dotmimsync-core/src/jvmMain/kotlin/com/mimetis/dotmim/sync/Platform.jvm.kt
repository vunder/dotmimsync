package com.mimetis.dotmimsync

class JvmPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override fun formatString(format: String, vararg args: String?): String =
        String.format(format, args)
}

actual fun getPlatform(): Platform = JvmPlatform()