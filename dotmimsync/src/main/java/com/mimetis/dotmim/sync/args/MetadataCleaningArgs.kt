package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.messages.MessageGetChangesBatch
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

class MetadataCleaningArgs(
    context: SyncContext,
    val setup: SyncSetup,
    val timeStampStart: Long
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug

    override val message: String
        get() = "Cleaning Metadatas."

    override val eventId: Int
        get() = 3000
}

/**
 * Intercept the provider action when a provider is cleaning metadata
 */
fun BaseOrchestrator.OnMetadataCleaning(action: (MetadataCleaningArgs) -> Unit) =
    this.setInterceptor(action)
