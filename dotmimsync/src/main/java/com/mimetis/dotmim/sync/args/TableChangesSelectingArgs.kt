package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

/**
 * Raise before selecting changes will occur
 */
class TableChangesSelectingArgs(
    context: SyncContext,

    /**
     * Gets the table from where the changes are going to be selected.
     */
    val table: SyncTable
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Debug
    override val message: String = "[${table.getFullName()}] Getting Changes."
    override val eventId: Int = 13000
}

/**
 * Intercept the provider action when changes are going to be selected on each table defined in the configuration schema
 */
fun BaseOrchestrator.onTableChangesSelecting(action: (TableChangesSelectingArgs) -> Unit) =
    this.setInterceptor(action)
