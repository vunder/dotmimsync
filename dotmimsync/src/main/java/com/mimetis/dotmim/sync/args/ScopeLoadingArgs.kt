package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbScopeType
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class ScopeLoadingArgs(
    context: SyncContext,
    val scopeName: String,
    val scopeType: DbScopeType
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Debug
    override val message: String = "Scope Table [$scopeType] Loading."
    override val eventId: Int = 7200
}

/**
 * Intercept the provider action when a scope is about to be loaded from client database
 */
fun BaseOrchestrator.onScopeLoading(action: (ScopeLoadingArgs) -> Unit) =
    this.setInterceptor(action)
