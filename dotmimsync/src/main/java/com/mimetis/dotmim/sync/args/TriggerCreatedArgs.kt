package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbTriggerType
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class TriggerCreatedArgs(
    context: SyncContext,
    val table: SyncTable,
    val triggerType: DbTriggerType
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Trace
    override val message: String = "[${table.getFullName()}] Trigger [${this.triggerType}] Created."
    override val eventId: Int = 15050
}

/**
 * Intercept the provider when a trigger is created
 */
fun BaseOrchestrator.onTriggerCreated(action: (TriggerCreatedArgs) -> Unit) =
    this.setInterceptor(action)
