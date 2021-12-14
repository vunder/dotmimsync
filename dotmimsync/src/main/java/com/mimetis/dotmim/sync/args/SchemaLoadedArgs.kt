package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncSet

class SchemaLoadedArgs(
    context: SyncContext,

    /**
     * Gets the schema loaded.
     */
    val schema: SyncSet
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Information

    override val message: String = "Schema Loaded For ${this.schema.tables.size} Tables."

    override val eventId: Int = 6050
}

/**
 * Intercept the provider when schema is loaded
 */
fun BaseOrchestrator.onSchemaLoaded(action: (SchemaLoadedArgs) -> Unit) =
    this.setInterceptor(action)
