package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Event args generated during BeginSession stage
 */
class SessionBeginArgs(
        context: SyncContext
) : ProgressArgs(context) {
    override val source: String
        get() = context.sessionId.toString()

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Information

    override val message: String
        get() = "Session Begins. Id:${context.sessionId}. Scope name:${context.scopeName}."

    override val eventId: Int
        get() = 100
}

/**
 * Intercept the provider action when session begin is called
 */
fun BaseOrchestrator.onSessionBegin(action: (SessionBeginArgs) -> Unit) =
    this.setInterceptor(action)
