package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.DbScopeType
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.scopes.ScopeInfo
import com.mimetis.dotmim.sync.scopes.ServerScopeInfo

class ScopeLoadedArgs<T>(
    context: SyncContext,
    val scopeName: String,
    val scopeType: DbScopeType,
    val scopeInfo: T
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Debug

    override val message: String
        get() = when (scopeInfo) {
            is ServerScopeInfo -> "[${scopeInfo.name}] [Version ${scopeInfo.version}] Last cleanup Timestamp:${scopeInfo.lastCleanupTimestamp}."
            is ScopeInfo -> "[${scopeInfo.name}] [Version ${scopeInfo.version}] Last sync:${scopeInfo.lastSync} Last sync duration:${scopeInfo.lastSyncDurationString}."
            else -> super.message
        }

    override val eventId: Int = 7250
}

/**
 * Intercept the provider action when a scope is loaded from client database
 */
fun BaseOrchestrator.onScopeLoaded(action: (ScopeLoadedArgs<ScopeInfo>) -> Unit) =
    this.setInterceptor(action)
