package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.batch.BatchInfo
import com.mimetis.dotmim.sync.messages.DatabaseChangesSelected
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Event args generated before after getting changes on the target database
 */
class DatabaseChangesSelectedArgs(
    context: SyncContext,
    val timestamp: Long?,

    /**
     * Get the batch info. Always null when raised from a call from GetEstimatedChangesCount
     */
    val clientBatchInfo: BatchInfo,
    val changesSelected: DatabaseChangesSelected
) : ProgressArgs(context) {
    override val message: String =
        "[Total] Upserts:${this.changesSelected.totalChangesSelectedUpdates}. Deletes:${this.changesSelected.totalChangesSelectedDeletes}. Total:${this.changesSelected.totalChangesSelected}"
    override val eventId: Int = 1050
}

/**
 * Occurs when changes have been retrieved from the local database
 */
fun BaseOrchestrator.onDatabaseChangesSelected(action: (DatabaseChangesSelectedArgs) -> Unit) =
    this.setInterceptor(action)
