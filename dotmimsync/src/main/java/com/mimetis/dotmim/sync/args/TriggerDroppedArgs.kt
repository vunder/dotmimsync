package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbTriggerType
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class TriggerDroppedArgs(
    context: SyncContext,
    val table: SyncTable,
    val triggerType: DbTriggerType
) : ProgressArgs(context) {
    override val message: String
        get() = "[${table.getFullName()}] Trigger [${this.triggerType}] Dropped."

    override val eventId: Int
        get() = 15150
}

/**
 * Intercept the provider when a trigger is dropped
 */
fun BaseOrchestrator.onTriggerDropped(action: (TriggerDroppedArgs) -> Unit) =
    this.setInterceptor(action)
