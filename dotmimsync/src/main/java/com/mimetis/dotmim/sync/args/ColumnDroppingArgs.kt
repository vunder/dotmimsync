package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.ParserName
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class ColumnDroppingArgs(
    context: SyncContext,
    val columnName: String,
    val table: SyncTable,
    val tableName: ParserName
):ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Trace

    override val message: String
        get() = "[$columnName] Dropping."

    override val eventId: Int
        get() = 12400
}

/**
 * Intercept the provider when a column is dropping
 */
fun BaseOrchestrator.onColumnDropping(action: (ColumnDroppingArgs) -> Unit) =
    this.setInterceptor(action)
