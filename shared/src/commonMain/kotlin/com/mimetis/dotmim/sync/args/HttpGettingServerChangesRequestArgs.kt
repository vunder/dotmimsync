package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class HttpGettingServerChangesRequestArgs(
    /**
     * Gets the batch index that is asked to be retrieved from the server
     */
    val batchIndexRequested: Int,

    /**
     * Gets the batch count to be received from server
     */
    val batchCount: Int,
    context: SyncContext,
    val host: String
) : ProgressArgs(context) {
    override val message: String =
        if (this.batchCount <= 1)
            "Getting Changes"
        else
            "Getting Batch Changes. (${this.batchIndexRequested + 1}/${this.batchCount})."

    override val source: String = host
    override val eventId: Int = 20100
}

/**
 * Intercept the provider when downloading a batch changes from server side.
 */
fun BaseOrchestrator.onHttpGettingChangesRequest(action: (HttpGettingServerChangesRequestArgs) -> Unit) =
    this.setInterceptor(action)
