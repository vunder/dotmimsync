package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.messages.MessageGetChangesBatch
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

internal class DbCommandArgs(
    context: SyncContext
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Sql

    override val message: String
        get() = "Sql Statement. [some sql]."

    override val eventId: Int
        get() = 9000
}
