package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbScopeType
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class ScopeSavedArgs(
        context: SyncContext,
        val scopeName: String,
        val scopeType: DbScopeType,
        val scopeInfo: Any
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug
    override val message: String = "Scope Table [$scopeType] Saved."
    override val eventId: Int = 7450
}

/**
 * Intercept the provider action when a scope is saved
 */
fun BaseOrchestrator.onScopeSaved(action: (ScopeSavedArgs) -> Unit) =
        this.setInterceptor(action)
