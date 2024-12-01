package com.mimetis.dotmimsync

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform