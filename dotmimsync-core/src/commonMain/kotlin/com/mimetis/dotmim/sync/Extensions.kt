package com.mimetis.dotmim.sync

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun utcNow(): Long = Clock.System.now().toEpochMilliseconds()
