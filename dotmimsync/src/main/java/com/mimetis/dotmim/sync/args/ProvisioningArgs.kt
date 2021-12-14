package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.enumerations.SyncProvision
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncSet
import java.util.*

class ProvisioningArgs(
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
    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug

    override val message: String
        get() = "Provisioning ${schema.tables.size} Tables. Provision:${provision}."

    override val eventId: Int
        get() = 5000
}

/**
 * Intercept the provider before it begins a database provisioning
 */
fun BaseOrchestrator.onProvisioning(action: (ProvisioningArgs) -> Unit) =
    this.setInterceptor(action)
