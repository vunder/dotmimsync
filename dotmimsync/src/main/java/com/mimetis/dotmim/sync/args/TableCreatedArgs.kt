package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.ParserName
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class TableCreatedArgs(
    context: SyncContext,
    val table: SyncTable,
    val tableName: ParserName
): ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Trace

    override val message: String
        get() = "[${table.getFullName()}] Table Created."

    override val eventId: Int
        get() = 12150
}

/**
 * Intercept the provider when a table is created
 */
fun BaseOrchestrator.onTableCreated(action: (TableCreatedArgs) -> Unit) =
    this.setInterceptor(action)
