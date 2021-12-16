package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.DataRowState
import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.batch.BatchPartInfo
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

/**
 * Event args before a batch changes is going to be applied on a datasource
 */
class TableChangesBatchApplyingArgs(
    context: SyncContext,

    /**
     * Gets the changes to be applied into the database
     */
    val batchPartInfo: BatchPartInfo,

    /**
     * Gets the RowState of the applied rows
     */
    val state: DataRowState
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Debug
    override val eventId: Int = 13100
    override val message: String = "Applying [${batchPartInfo.fileName}] Batch file. State:$state."
}

/**
 * Intercept the provider action when a batch changes is going to be applied on a table
 */
fun BaseOrchestrator.onTableChangesBatchApplying(action: (TableChangesBatchApplyingArgs) -> Unit) =
    this.setInterceptor(action)
