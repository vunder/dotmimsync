package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class SchemaNameCreatedArgs(
    context: SyncContext,
    val table: SyncTable
): ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Trace

    override val message: String
        get() = "[${this.table.schemaName}] Schema Created."

    override val eventId: Int
        get() = 12050
}

/**
 * Intercept the provider when database schema is created (works only on SQL Server)
 */
fun BaseOrchestrator.onSchemaNameCreated(action: (SchemaNameCreatedArgs) -> Unit) =
    this.setInterceptor(action)
