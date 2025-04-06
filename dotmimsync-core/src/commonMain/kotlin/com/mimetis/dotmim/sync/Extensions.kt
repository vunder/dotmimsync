package com.mimetis.dotmim.sync

import kotlinx.datetime.Clock

fun utcNow(): Long = Clock.System.now().toEpochMilliseconds()
