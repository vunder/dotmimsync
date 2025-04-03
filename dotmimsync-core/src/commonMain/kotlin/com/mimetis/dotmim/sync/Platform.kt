package com.mimetis.dotmimsync

interface Platform {
    val name: String
    fun formatString(format: String, vararg args: Any?): String
}

expect fun getPlatform(): Platform

private var platform: Platform? = null

val currentPlatform: Platform
    get() {
        if (platform == null)
            platform = getPlatform()
        return platform!!
    }