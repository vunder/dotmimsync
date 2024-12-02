package com.mimetis.dotmim.sync

interface Progress<T> {
    fun report(value: T)
}
