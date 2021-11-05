package com.mimetis.dotmim.sync

import kotlin.math.pow

object SyncVersion {
    const val current: String = "0.8.1"
    fun ensureVersion(v: String): String =
            if (v == "1") "0.5.7" else v

    fun String.toVersionInt(): Int =
            this.split('.').reversed().mapIndexed { index, s -> (s.toInt() * 10.0.pow(index * 2)).toInt() }.sum()

    fun String.major(): Int =
            this.split('.')[0].toInt()

    fun String.minor(): Int =
            this.split('.')[1].toInt()

    fun String.build(): Int =
            this.split('.')[2].toInt()
}
