package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.messages.TableChangesSelected
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

/**
 * Contains statistics about selected changes from local provider
 */
class TableChangesSelectedArgs(
    context: SyncContext,
    /**
     * Gets the SyncTable instances containing all changes selected.
     * If you get this instance from a call from GetEstimatedChangesCount, this property is always null
     */
    val changes: SyncTable,

    /**
     * Gets the incremental summary of changes selected
     */
    val tableChangesSelected: TableChangesSelected
) : ProgressArgs(context) {
    override val message: String =
        "[${this.tableChangesSelected.tableName}] [Total] Upserts:${this.tableChangesSelected.upserts}. Deletes:${this.tableChangesSelected.deletes}. Total:${this.tableChangesSelected.totalChanges}."

    override val eventId: Int = 13050
}

/**
 * Intercept the provider action when changes are selected on each table defined in the configuration schema
 */
fun BaseOrchestrator.onTableChangesSelected(action: (TableChangesSelectedArgs) -> Unit) =
    this.setInterceptor(action)
