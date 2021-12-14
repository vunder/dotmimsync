package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.enumerations.SyncProgressLevel
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.setup.MigrationResults
import com.mimetis.dotmim.sync.setup.SyncSetup

/**
 * Object representing a migration about to start
 */
class MigratedArgs(
    context: SyncContext,

    /**
     * Gets the schema currently used
     */
    val schema: SyncSet,

    /**
     * Gets the new setup applied
     */
    val setup: SyncSetup,

    /**
     *  Gets the Migration results
     */
    val migrationResults: MigrationResults
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug

    override val message: String
        get() = "Migrated. Tables:${setup.tables.size}."

    override val eventId: Int
        get() = 4050
}

/**
 * Intercept the orchestrator when a Setup has been migrated
 */
fun BaseOrchestrator.onMigrated(action: (MigratedArgs) -> Unit) =
    this.setInterceptor(action)
