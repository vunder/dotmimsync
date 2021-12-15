package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Event args generated before applying a snapshot on the target database
 */
class SnapshotApplyingArgs(
    context: SyncContext
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Debug
    override val message: String = "Applying Snapshot."
    override val eventId: Int = 10100
}

/**
 * Intercept the orchestrator when applying a snapshot
 */
fun BaseOrchestrator.onSnapshotApplying(action: (SnapshotApplyingArgs) -> Unit) =
    this.setInterceptor(action)
