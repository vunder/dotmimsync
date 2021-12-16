package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbTriggerType
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class TriggerDroppingArgs(
    context: SyncContext,
    val table: SyncTable,
    val triggerType: DbTriggerType
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Trace
    override val message: String =
        "[${table.getFullName()}] Trigger [${this.triggerType}] Dropping."
    override val eventId: Int = 15100
}

/**
 * Intercept the provider when a trigger is dropping
 */
fun BaseOrchestrator.onTriggerDropping(action: (TriggerDroppingArgs) -> Unit) =
    this.setInterceptor(action)

