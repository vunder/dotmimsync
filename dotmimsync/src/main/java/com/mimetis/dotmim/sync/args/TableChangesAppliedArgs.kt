package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.messages.TableChangesApplied
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Event args raised when all changes for a table have been applied on a datasource
 */
class TableChangesAppliedArgs(
    context: SyncContext,

    val tableChangesApplied: TableChangesApplied
) : ProgressArgs(context) {
    override val message: String
        get() = "[${tableChangesApplied.tableName}] Changes ${tableChangesApplied.state} Applied:${tableChangesApplied.applied}. Resolved Conflicts:${tableChangesApplied.resolvedConflicts}."

    override val eventId: Int
        get() = 13150
}

/**
 * Intercept the provider action when a all changes have been applied on a datasource table
 */
fun BaseOrchestrator.onTableChangesApplied(action: (TableChangesAppliedArgs) -> Unit) =
    this.setInterceptor(action)
