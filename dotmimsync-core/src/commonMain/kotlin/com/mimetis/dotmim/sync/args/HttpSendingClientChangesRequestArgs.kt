package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.web.client.HttpMessageSendChangesRequest

class HttpSendingClientChangesRequestArgs(
    val request: HttpMessageSendChangesRequest,

    /**
     * Gets or Sets the rows count sended
     */
    val rowsCount: Int,

    /**
     * Gets or Sets the total tables rows count to send
     */
    val totalRowsCount: Int,
    val host: String
) : ProgressArgs(request.syncContext) {
    override val eventId: Int = 20000
    override val source: String = this.host
    override val message: String =
        if (this.request.batchCount == 0 && this.request.batchIndex == 0)
            "Sending All Changes. Rows:${this.rowsCount}. Waiting Server Response..."
        else
            "Sending Batch Changes. Batches: (${this.request.batchIndex + 1}/${this.request.batchCount}). Rows: (${this.rowsCount}/${this.totalRowsCount}). Waiting Server Response..."
}

/**
 * Intercept the provider when batch changes is uploading to server.
 */
fun BaseOrchestrator.onHttpSendingChangesRequest(action: (HttpSendingClientChangesRequestArgs) -> Unit) =
    this.setInterceptor(action)
