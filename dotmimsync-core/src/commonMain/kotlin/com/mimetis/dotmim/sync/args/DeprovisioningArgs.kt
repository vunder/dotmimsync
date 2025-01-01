package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProvision
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncSet
import java.util.*

class DeprovisioningArgs(
    context: SyncContext,

    /**
     * Get the provision type (Flag enum)
     */
    val provision: EnumSet<SyncProvision>,

    /**
     * Gets the schema to be applied in the database
     */
    val schema: SyncSet
) : ProgressArgs(context) {
    override val message: String =
        "Deprovisioning ${schema.tables.size} Tables. Provision:$provision."
    override val eventId: Int = 5100
}

/**
 * Intercept the provider before it begins a database deprovisioning
 */
fun BaseOrchestrator.onDeprovisioning(action: (DeprovisioningArgs) -> Unit) =
    this.setInterceptor(action)
