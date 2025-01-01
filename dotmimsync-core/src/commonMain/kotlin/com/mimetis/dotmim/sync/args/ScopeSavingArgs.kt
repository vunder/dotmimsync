package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbScopeType

class ScopeSavingArgs(
    context: SyncContext,
    val scopeName: String,
    val scopeType: DbScopeType,
    val scopeInfo: Any
) : ProgressArgs(context) {
    override val eventId: Int = 7400
    override val message: String = "Scope Table [$scopeType] Saving."
}
