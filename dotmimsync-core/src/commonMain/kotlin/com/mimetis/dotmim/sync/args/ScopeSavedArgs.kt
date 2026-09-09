package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbScopeType

class ScopeSavedArgs(
    context: SyncContext,
    val scopeName: String,
    val scopeType: DbScopeType,
    val scopeInfo: Any
) : ProgressArgs(context) {
    override val eventId: Int = 7450
    override val message: String = "Scope Table [$scopeType] Saved."
}
