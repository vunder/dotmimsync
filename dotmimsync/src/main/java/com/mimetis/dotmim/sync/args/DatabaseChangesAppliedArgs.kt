package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.messages.DatabaseChangesApplied
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Event args generated after changes applied
 */
class DatabaseChangesAppliedArgs(
    context: SyncContext,
    val changesApplied: DatabaseChangesApplied
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel
        get() = if (changesApplied.totalAppliedChanges > 0) SyncProgressLevel.Information else SyncProgressLevel.Debug

    override val message: String
        get() = "[Total] Applied:${changesApplied.totalAppliedChanges}. Conflicts:${changesApplied.totalResolvedConflicts}."

    override val eventId: Int
        get() = 1150
}

/**
 * Intercept the provider action when changes are applied on each table defined in the configuration schema
 */
fun BaseOrchestrator.onDatabaseChangesApplied(action: (DatabaseChangesAppliedArgs) -> Unit) =
    this.setInterceptor(action)
