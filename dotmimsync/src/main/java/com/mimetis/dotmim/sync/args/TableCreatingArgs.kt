package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.ParserName
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class TableCreatingArgs(
    context: SyncContext,
    val table: SyncTable,
    val tableName: ParserName
): ProgressArgs(context) {
    var cancel: Boolean = false

    override val message: String
        get() = "[${table.getFullName()}] Table Creating."

    override val eventId: Int
        get() = 12100
}

/**
 * Intercept the provider when a table is creating
 */
fun BaseOrchestrator.onTableCreating(action: (TableCreatingArgs) -> Unit) =
    this.setInterceptor(action)
