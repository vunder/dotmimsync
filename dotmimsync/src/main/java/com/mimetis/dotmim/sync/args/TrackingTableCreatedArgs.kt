package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.builders.ParserName
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncTable

class TrackingTableCreatedArgs(
    context: SyncContext,
    val table: SyncTable,
    val trackingTableName: ParserName
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Trace

    override val message: String
        get() = "[${this.trackingTableName}] tracking table created."

    override val eventId: Int
        get() = 14050
}

/**
 * Intercept the provider when a tracking table is created
 */
fun BaseOrchestrator.onTrackingTableCreated(action: (TrackingTableCreatedArgs) -> Unit) =
    this.setInterceptor(action)
