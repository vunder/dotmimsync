package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.ParserName
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class TableDroppingArgs(
    context: SyncContext,
    val table: SyncTable,
    val tableName: ParserName
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Trace
    override val message: String = "[${table.getFullName()}] Table Dropping."
    override val eventId: Int = 12200
}


/**
 *  Intercept the provider when a table is dropping
 */
fun BaseOrchestrator.onTableDropping(action: (TableDroppingArgs) -> Unit) =
    this.setInterceptor(action)
