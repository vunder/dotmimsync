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
class MigratingArgs(
    context: SyncContext,

    /**
     * Gets the schema used to apply migration
     */
    val newSchema: SyncSet,

    /**
     * Gets the old setup to migrate
     */
    val oldSetup: SyncSetup,

    /**
     *  Gets the new setup to apply
     */
    val newSetup: SyncSetup,
    val migrationResults: MigrationResults
) : ProgressArgs(context) {
    var cancel: Boolean = false

    override val progressLevel: SyncProgressLevel
        get() = SyncProgressLevel.Debug

    override val message: String
        get() = "Applying Migration."

    override val eventId: Int
        get() = 4000
}

/**
 * Intercept the orchestrator when migrating a Setup
 */
fun BaseOrchestrator.onMigrating(action: (MigratingArgs) -> Unit) =
    this.setInterceptor(action)
