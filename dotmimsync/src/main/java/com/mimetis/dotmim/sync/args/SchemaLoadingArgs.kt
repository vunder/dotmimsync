package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.setup.SyncSetup

class SchemaLoadingArgs(
    context: SyncContext,

    /**
     * Gets the Setup to be load.
     */
    val setup: SyncSetup
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug

    override val message: String = "Loading Schema For ${this.setup.tables.size} Tables."

    override val eventId: Int = 6000
}

/**
 * Intercept the provider when schema is loading
 */
fun BaseOrchestrator.onSchemaLoading(action: (SchemaLoadingArgs) -> Unit) =
    this.setInterceptor(action)
