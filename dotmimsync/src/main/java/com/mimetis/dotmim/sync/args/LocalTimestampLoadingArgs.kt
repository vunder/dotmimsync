package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.messages.MessageGetChangesBatch
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class LocalTimestampLoadingArgs(
    context: SyncContext
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug

    override val message: String
        get() = "Getting Local Timestamp."

    override val eventId: Int
        get() = 2000
}

/**
 * Intercept the provider action when a database is reading a timestamp
 */
fun BaseOrchestrator.onLocalTimestampLoading(action: (LocalTimestampLoadingArgs) -> Unit) =
    this.setInterceptor(action)
