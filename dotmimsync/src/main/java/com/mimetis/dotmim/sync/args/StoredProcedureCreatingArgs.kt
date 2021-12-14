package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbStoredProcedureType
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class StoredProcedureCreatingArgs(
    context: SyncContext,
    val table: SyncTable,
    val storedProcedureType: DbStoredProcedureType
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val message: String
        get() = "[${this.table.getFullName()}] Stored Procedure [${this.storedProcedureType}] Creating."

    override val eventId: Int
        get() = 11000
}

/**
 * Intercept the provider when a Stored Procedure is creating
 */
fun BaseOrchestrator.onStoredProcedureCreating(action: (StoredProcedureCreatingArgs) -> Unit) =
    this.setInterceptor(action)
