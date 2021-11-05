package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.DataRowState
import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

/**
 * Event args before a table changes is going to be applied on a datasource
 */
class TableChangesApplyingArgs(
    context: SyncContext,

    /**
     * Gets the changes to be applied into the database
     */
    val table: SyncTable,

    /**
     * Gets the RowState of the applied rows
     */
    val state: DataRowState
) : ProgressArgs(context) {
    override val message: String
        get() = "Applying Changes To ${table.getFullName()}"

    override val eventId: Int
        get() = 13100

    var cancel = false
}

/**
 * Intercept the provider action when a table starts to apply changes
 */
fun BaseOrchestrator.onTableChangesApplying(action: (TableChangesApplyingArgs) -> Unit) =
    this.setInterceptor(action)
