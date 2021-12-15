package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbScopeType
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class ScopeSavingArgs(
        context: SyncContext,
        val scopeName: String,
        val scopeType: DbScopeType,
        val scopeInfo: Any
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug
    override val eventId: Int = 7400
    override val message: String = "Scope Table [$scopeType] Saving."
}

/**
 * Intercept the provider action when a scope is saving
 */
fun BaseOrchestrator.onScopeSaving(action: (ScopeSavingArgs) -> Unit) =
        this.setInterceptor(action)
