package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.batch.BatchInfo
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Represents a request made to the server to get the server scope info
 */
class HttpBatchesDownloadingArgs(
    context: SyncContext,
    val startTime: Long,
    val serverBatchInfo: BatchInfo,
    val host: String
) : ProgressArgs(context) {
    override val message: String =
        "Downloading Batches. Scope Name:${this.context.scopeName}. Batches Count:${this.serverBatchInfo.batchPartsInfo?.size ?: 1}. Rows Count:${this.serverBatchInfo.rowsCount}"
    override val eventId: Int = 20200
    override val source: String = host
}

/**
 * Intercept the provider when batches are about to be downloaded
 */
fun BaseOrchestrator.onHttpBatchesDownloadingArgs(action: (HttpBatchesDownloadingArgs) -> Unit) =
    this.setInterceptor(action)
