package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class SchemaNameCreatingArgs(
    context: SyncContext,
    val table: SyncTable
): ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Trace

    override val message: String
        get() = "[${this.table.schemaName}] Schema Creating."

    override val eventId: Int
        get() = 12000
}

/**
 * Intercept the provider when database schema is creating (works only on SQL Server)
 */
fun BaseOrchestrator.onSchemaNameCreating(action: (SchemaNameCreatingArgs) -> Unit) =
    this.setInterceptor(action)
