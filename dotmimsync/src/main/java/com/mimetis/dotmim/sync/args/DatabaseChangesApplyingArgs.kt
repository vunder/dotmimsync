package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.messages.MessageApplyChanges
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class DatabaseChangesApplyingArgs(
    context: SyncContext,

    /**
     * All parameters that will be used to apply changes
     */
    val applyChanges: MessageApplyChanges
):ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug

    override val message: String
        get() = "Applying Changes. Total Changes To Apply: ${applyChanges.changes.rowsCount}"

    override val eventId: Int
        get() = 1100
}

/**
 * Intercept the provider action when changes are going to be applied on each table defined in the configuration schema
 */
fun BaseOrchestrator.onDatabaseChangesApplying(action: (DatabaseChangesApplyingArgs) -> Unit) =
    this.setInterceptor(action)
