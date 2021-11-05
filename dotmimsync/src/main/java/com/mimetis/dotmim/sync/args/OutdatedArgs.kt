package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.scopes.ScopeInfo
import com.mimetis.dotmim.sync.scopes.ServerScopeInfo

class OutdatedArgs(
    context: SyncContext,

    /**
     * Gets the client scope info used to check if the client is outdated
     */
    val clientScopeInfo: ScopeInfo,

    /**
     * Gets the server scope info to check if client is outdated
     */
    val serverScopeInfo: ServerScopeInfo
) : ProgressArgs(context) {
    override val message: String=
        "Database Out Dated. Last Client Sync Endpoint ${clientScopeInfo.lastServerSyncTimestamp} < Last Server Cleanup Metadatas ${serverScopeInfo.lastCleanupTimestamp}."
    override val eventId: Int =
        5000

    /**
     * Gets or sets an action enumeration value for the action to handle the outdated peer.
     */
    var action: OutdatedAction = OutdatedAction.Rollback
}

enum class OutdatedAction {
    /**
     * Reinitialize the whole sync database, applying all rows from the server to the client
     */
    Reinitialize,

    /**
     * Reinitialize the whole sync database, applying all rows from the server to the client, after trying a client upload
     */
    ReinitializeWithUpload,

    /**
     * Rollback the synchronization request.
     */
    Rollback
}

/**
 * Intercept the provider action when a database is out dated
 */
fun BaseOrchestrator.onOutdated(action: (OutdatedArgs) -> Unit) =
    this.setInterceptor(action)
