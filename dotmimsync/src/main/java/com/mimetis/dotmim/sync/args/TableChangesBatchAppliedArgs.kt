package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.messages.TableChangesApplied
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Event args raised when a batch changes is applied on a datasource
 */
class TableChangesBatchAppliedArgs(
    context: SyncContext,

    /**
     * Table changes applied
     */
    val tableChangesApplied: TableChangesApplied
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Debug
    override val message: String =
        "[${this.tableChangesApplied.tableName}] [${this.tableChangesApplied.state}] " +
                "Applied:(${this.tableChangesApplied.applied}) Total:(${this.tableChangesApplied.totalAppliedCount}/${this.tableChangesApplied.totalRowsCount.toDouble()})."

    override val eventId: Int = 13150
}

/**
 * Intercept the provider action when a batch changes is applied on a datasource table
 */
fun BaseOrchestrator.onTableChangesBatchApplied(action: (TableChangesBatchAppliedArgs) -> Unit) =
    this.setInterceptor(action)
