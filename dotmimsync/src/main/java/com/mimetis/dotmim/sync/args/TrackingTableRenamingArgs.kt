package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.ParserName
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class TrackingTableRenamingArgs(
    context: SyncContext,
    val table: SyncTable,
    val trackingTableName: ParserName,
    val oldTrackingTableName: ParserName
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel = SyncProgressLevel.Trace
    override val message: String = "[${this.trackingTableName}] Tracking Table Renaming."
    override val eventId: Int = 14200
}

/**
 * Intercept the provider when a tracking table is renaming
 */
fun BaseOrchestrator.onTrackingTableRenaming(action: (TrackingTableRenamingArgs) -> Unit) =
    this.setInterceptor(action)
