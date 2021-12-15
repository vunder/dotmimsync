package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbScopeType
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class ScopeTableDroppingArgs (
    context: SyncContext,
    val scopeName: String,
    val scopeType: DbScopeType
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Trace
    override val message: String = "Scope Table [$scopeType] Dropping."
    override val eventId: Int = 7100
}

/**
 * Intercept the provider action when a scope table is dropping
 */
fun BaseOrchestrator.onScopeTableDropping(action: (ScopeTableDroppingArgs ) -> Unit) =
    this.setInterceptor(action)
