package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.messages.MessageGetChangesBatch

/**
 * Event args generated before getting changes on the target database
 */
class DatabaseChangesSelectingArgs(
    context: SyncContext,
    val changesRequest: MessageGetChangesBatch
) : ProgressArgs(context) {
    override val message: String
        get() = "Getting Changes."

    override val eventId: Int
        get() = 1000
}