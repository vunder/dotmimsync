package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.ParserName
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class TrackingTableRenamedArgs(
    context: SyncContext,
    val table: SyncTable,
    val trackingTableName: ParserName,
    val oldTrackingTableName: ParserName
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Trace
    override val message: String = "[${this.trackingTableName}] Tracking Table Renamed."
    override val eventId: Int = 14250
}

/**
 * Intercept the provider when a tracking table is renamed
 */
fun BaseOrchestrator.onTrackingTableRenamed(action: (TrackingTableRenamedArgs) -> Unit) =
    this.setInterceptor(action)
