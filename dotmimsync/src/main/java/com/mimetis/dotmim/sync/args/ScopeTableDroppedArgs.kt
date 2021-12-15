package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbScopeType
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class ScopeTableDroppedArgs(
    context: SyncContext,
    val scopeName: String,
    val scopeType: DbScopeType
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Trace
    override val message: String = "Scope Table [$scopeType] Dropped."
    override val eventId: Int = 7150
}

/**
 * Intercept the provider action when a scope table is dropped
 */
fun BaseOrchestrator.onScopeTableDropped(action: (ScopeTableDroppedArgs) -> Unit) =
    this.setInterceptor(action)
