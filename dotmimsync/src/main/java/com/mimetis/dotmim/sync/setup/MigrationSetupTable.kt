package com.mimetis.dotmim.sync.setup

import com.mimetis.dotmim.sync.MigrationTableDropNotAllowedException

class MigrationSetupTable(
    /**
     * Table to migrate
     */
    var setupTable: SetupTable
) {
    private var _table: MigrationAction = MigrationAction.None

    /**
     * Gets or Sets a boolean indicating that this table should recreate the stored procedures
     */
    var storedProcedures: MigrationAction = MigrationAction.None


    /**
     * Gets or Sets a boolean indicating that this table should recreate triggers
     */
    var triggers: MigrationAction = MigrationAction.None

    /**
     * Gets or Sets a boolean indicating that this table should recreate the tracking table
     */
    var trackingTable: MigrationAction = MigrationAction.None

    /**
     * Gets a value indicating if the table should be migrated
     */
    val shouldMigrate: Boolean
        get() = this.trackingTable != MigrationAction.None
                || this.triggers != MigrationAction.None
                || this.storedProcedures != MigrationAction.None
                || this.table != MigrationAction.None

    /**
     * Gets or Sets a boolean indicating that this table should be recreated
     */
    var table: MigrationAction
        get() = _table
        set(value) {
            if (value == MigrationAction.Drop)
                throw MigrationTableDropNotAllowedException()
            _table = value
        }
}