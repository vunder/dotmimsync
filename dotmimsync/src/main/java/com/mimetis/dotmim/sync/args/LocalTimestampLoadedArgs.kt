package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.messages.MessageGetChangesBatch
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class LocalTimestampLoadedArgs(
    context: SyncContext,
    val localTimestamp: Long
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug

    override val message: String
        get() = "Local Timestamp Loaded:$localTimestamp."

    override val eventId: Int
        get() = 2050
}

/**
 * Intercept the provider action when a database has read a timestamp
 */
fun BaseOrchestrator.onLocalTimestampLoaded(action: (LocalTimestampLoadedArgs) -> Unit) =
    this.setInterceptor(action)
