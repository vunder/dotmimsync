package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProvision
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncSet
import java.util.*

class ProvisionedArgs(
    context: SyncContext,
    val provision: EnumSet<SyncProvision>,
    val schema: SyncSet
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Information

    override val message: String
        get() = "Provisioned ${schema.tables.size} Tables. Provision:${provision}."

    override val eventId: Int
        get() = 5050
}


/**
 * Intercept the provider after it has provisioned a database
 */
fun BaseOrchestrator.onProvisioned(action: (ProvisionedArgs) -> Unit) =
    this.setInterceptor(action)
