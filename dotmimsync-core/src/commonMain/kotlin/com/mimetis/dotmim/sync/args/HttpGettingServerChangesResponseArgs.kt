package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.batch.BatchInfo
import com.mimetis.dotmim.sync.messages.DatabaseChangesApplied
import com.mimetis.dotmim.sync.messages.DatabaseChangesSelected
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.ContainerSet
import com.mimetis.dotmim.sync.web.client.HttpMessageSendChangesResponse

class HttpGettingServerChangesResponseArgs(
    val batchInfo: BatchInfo,
    val batchIndex: Int,
    val batchRowsCount: Int,
    syncContext: SyncContext,
    val host: String,
) : ProgressArgs(syncContext) {
    override val source: String = this.host
    override val eventId: Int = 20150

    override val message: String
        get() {
            val batchesCount = this.batchInfo.batchPartsInfo?.size ?: 1
            return "Downloaded Batch Changes. (${this.batchIndex + 1}/${batchesCount}). Rows:($batchRowsCount/${this.batchInfo.rowsCount})."
        }
}

/**
 * Intercept the provider when a batch changes has been downloaded from server side
 */
fun BaseOrchestrator.onHttpGettingChangesResponse(action: (HttpGettingServerChangesResponseArgs) -> Unit) =
    this.setInterceptor(action)
