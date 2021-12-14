package com.mimetis.dotmim.sync.setup

class MigrationResults {
    /**
     * Gets or Sets a boolean indicating that all tables should recreate their own stored procedures
     */
    var allStoredProcedures: MigrationAction = MigrationAction.None

    /**
     * Gets or Sets a boolean indicating that all tables should recreate their own triggers
     */
    var allTriggers: MigrationAction = MigrationAction.None

    /**
     * Gets or Sets a boolean indicating that all tables should recreate their tracking table
     */
    var allTrackingTables: MigrationAction = MigrationAction.None

    /**
     * Tables involved in the migration
     */
    var tables: MutableList<MigrationSetupTable> = ArrayList()
}