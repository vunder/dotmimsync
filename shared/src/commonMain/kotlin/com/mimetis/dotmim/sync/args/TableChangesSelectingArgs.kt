package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.set.SyncTable

/**
 * Raise before selecting changes will occur
 */
class TableChangesSelectingArgs(
    context: SyncContext,
    schemaTable: SyncTable
):ProgressArgs(context) {
    override val message: String
        get() = "Getting Changes [${table.getFullName()}]."

    override val eventId: Int
        get() = 13000

    /**
     * Gets the table from where the changes are going to be selected.
     */
    val table: SyncTable = schemaTable

    var cancel: Boolean = false
}