package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.enumerations.SyncProvision
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncSet
import java.util.*

class DeprovisionedArgs(
    context: SyncContext,
    val provision: EnumSet<SyncProvision>,
    val schema: SyncSet
) : ProgressArgs(context) {
    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Information

    override val message: String =
        "Deprovisioned ${schema.tables.size} Tables. Provision: $provision."

    override val eventId: Int = 5150
}


/**
 * Intercept the provider after it has deprovisioned a database
 */
fun BaseOrchestrator.onDeprovisioned(action: (DeprovisionedArgs) -> Unit) =
    this.setInterceptor(action)
