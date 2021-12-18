package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbTriggerType
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class TriggerCreatingArgs(
    context: SyncContext,
    val table: SyncTable,
    val triggerType: DbTriggerType
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Trace
    override val message: String = "[${table.getFullName()}] Trigger [${this.triggerType}] Creating."
    override val eventId: Int = 15000
}

/**
 * Intercept the provider when a trigger is creating
 */
fun BaseOrchestrator.onTriggerCreating(action: (TriggerCreatingArgs) -> Unit) =
    this.setInterceptor(action)
