package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.SyncResult
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.web.client.HttpMessageSummaryResponse

class HttpBatchesDownloadedArgs(
    val httpSummary: HttpMessageSummaryResponse,
    context: SyncContext,
    val startTime: Long,
    val completeTime: Long,
    val host: String
) : ProgressArgs(context) {
    override val eventId: Int = 20250

    override val source: String = host

    override val message: String
        get() {
            val batchCount = this.httpSummary.batchInfo?.batchPartsInfo?.size ?: 1
            val totalRows = this.httpSummary.serverChangesSelected?.totalChangesSelected ?: 0

            return "Snapshot Downloaded. Batches Count: $batchCount. Total Rows: $totalRows. Duration: $duration"
        }

    val duration: String = SyncResult.getVConfDuration(startTime, completeTime)
}

/**
 * Intercept the provider when batches have been completely downloaded
 */
fun BaseOrchestrator.onHttpBatchesDownloadedArgs(action: (HttpBatchesDownloadedArgs) -> Unit) =
    this.setInterceptor(action)

