package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbStoredProcedureType
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class StoredProcedureCreatedArgs(
    context: SyncContext,
    val table: SyncTable,
    val storedProcedureType: DbStoredProcedureType
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Trace

    override val message: String
        get() = "[${this.table.getFullName()}] Stored Procedure [${this.storedProcedureType}] Created."

    override val eventId: Int
        get() = 11050
}

/**
 * Intercept the provider when a Stored Procedure is created
 */
fun BaseOrchestrator.onStoredProcedureCreated(action: (StoredProcedureCreatedArgs) -> Unit) =
    this.setInterceptor(action)
