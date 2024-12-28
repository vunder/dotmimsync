package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import kotlin.uuid.ExperimentalUuidApi

/**
 * Event args generated during EndSession stage
 */
@OptIn(ExperimentalUuidApi::class)
class SessionEndArgs(
    context: SyncContext
) : ProgressArgs(context) {
    override val source: String
        get() = context.sessionId.toString()

    override val message: String
        get() = "Session Ended."

    override val eventId: Int
        get() = 200
}

fun BaseOrchestrator.onSessionEnd(action: (SessionEndArgs) -> Unit) =
    this.setInterceptor(action)
