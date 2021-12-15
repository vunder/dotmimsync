package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.messages.DatabaseChangesApplied
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Event args generated before applying a snapshot on the target database
 */
class SnapshotAppliedArgs(
    context: SyncContext,
    var changesApplied: DatabaseChangesApplied
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Information
    override val source: String = "Snapshot"
    override val message: String =
        "[Total] Applied:${changesApplied.totalAppliedChanges}. Resolved Conflicts:${changesApplied.totalResolvedConflicts}."
    override val eventId: Int = 10150
}

/**
 * Intercept the orchestrator when a snapshot has been applied
 */
fun BaseOrchestrator.onSnapshotApplied(action: (SnapshotAppliedArgs) -> Unit) =
    this.setInterceptor(action)
