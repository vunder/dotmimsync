package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbScopeType
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class ScopeTableCreatedArgs(
    context: SyncContext,
    val scopeName: String,
    val scopeType: DbScopeType
) : ProgressArgs(context) {
    override val eventId: Int = 7050
    override val message: String = "Scope Table [$scopeType] Created."
}

/**
 * Intercept the provider action when a scope table is created
 */
fun BaseOrchestrator.onScopeTableCreated(action: (ScopeTableCreatedArgs) -> Unit) =
    this.setInterceptor(action)
