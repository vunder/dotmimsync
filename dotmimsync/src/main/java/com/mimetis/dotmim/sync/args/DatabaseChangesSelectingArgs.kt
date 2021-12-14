package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.messages.MessageGetChangesBatch
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Event args generated before getting changes on the target database
 */
class DatabaseChangesSelectingArgs(
    context: SyncContext,
    val changesRequest: MessageGetChangesBatch
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug

    override val message: String
        get() = "Getting Changes. [${changesRequest.batchDirectory}]. Batch size:${changesRequest.batchSize}. IsNew:${changesRequest.isNew}. LastTimestamp:${changesRequest.lastTimestamp}."

    override val eventId: Int
        get() = 1000
}

/**
 * Occurs when changes are going to be queried on the local database
 */
fun BaseOrchestrator.onDatabaseChangesSelecting(action: (DatabaseChangesSelectingArgs) -> Unit) =
    this.setInterceptor(action)
