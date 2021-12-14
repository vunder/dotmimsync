package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.messages.DatabaseMetadatasCleaned
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class MetadataCleanedArgs(
    context: SyncContext,

    /**
     * Gets or Sets the rows count cleaned for all tables, during a DeleteMetadatasAsync call
     */
    var databaseMetadatasCleaned: DatabaseMetadatasCleaned
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Information

    override val message: String
        get() = "Tables Cleaned:${databaseMetadatasCleaned.tables.size}. Rows Cleaned:${databaseMetadatasCleaned.rowsCleanedCount}."

    override val eventId: Int
        get() = 3050
}

/**
 * Intercept the provider action when a provider has cleaned metadata
 */
fun BaseOrchestrator.OnMetadataCleaned(action: (MetadataCleanedArgs) -> Unit) =
    this.setInterceptor(action)
