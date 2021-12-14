package com.mimetis.dotmim.sync.setup

class Migration(
    private val newSetup: SyncSetup,
    private val oldSetup: SyncSetup
) {
    fun compare(): MigrationResults {
        val migrationSetup = MigrationResults()

        if (newSetup.equalsByProperties(oldSetup))
            return migrationSetup

        // if we change the prefix / suffix, we should recreate all stored procedures
        if (!newSetup.storedProceduresPrefix.equals(oldSetup.storedProceduresPrefix, true) || !newSetup.storedProceduresSuffix.equals(oldSetup.storedProceduresSuffix, true))
            migrationSetup.allStoredProcedures = MigrationAction.Create

        // if we change the prefix / suffix, we should recreate all triggers
        if (!newSetup.triggersPrefix.equals(oldSetup.triggersPrefix, true) || !newSetup.triggersSuffix.equals(oldSetup.triggersSuffix, true))
            migrationSetup.allTriggers = MigrationAction.Create

        // If we change tracking tables prefix and suffix, we should:
        // - RENAME the tracking tables (and keep the rows)
        // - RECREATE the stored procedure
        // - RECREATE the triggers
        if (!newSetup.trackingTablesPrefix.equals(oldSetup.trackingTablesPrefix, true) || !newSetup.trackingTablesSuffix.equals(oldSetup.trackingTablesSuffix, true))
        {
            migrationSetup.allStoredProcedures = MigrationAction.Create
            migrationSetup.allTriggers = MigrationAction.Create
            migrationSetup.allTrackingTables = MigrationAction.Rename
        }

        // Search for deleted tables
        val deletedTables = oldSetup.tables.filter { oldt -> newSetup.tables[oldt.tableName, oldt.schemaName] == null }

        // We found some tables present in the old setup, but not in the new setup
        // So, we are removing all the sync elements from the table, but we do not remote the table itself
        deletedTables.forEach { deletedTable ->
            val migrationDeletedSetupTable = MigrationSetupTable(deletedTable).apply {
                storedProcedures = MigrationAction.Drop
                trackingTable = MigrationAction.Drop
                triggers = MigrationAction.Drop
                table = MigrationAction.None
            }

            migrationSetup.tables.add(migrationDeletedSetupTable)
        }

        // Search for new tables
        val newTables = newSetup.tables.filter { newdt -> oldSetup.tables[newdt.tableName, newdt.schemaName] == null }

        // We found some tables present in the new setup, but not in the old setup
        newTables.forEach { newTable ->
            val migrationAddedSetupTable = MigrationSetupTable(newTable).apply {
                storedProcedures = MigrationAction.Create
                trackingTable = MigrationAction.Create
                triggers = MigrationAction.Create
                table = MigrationAction.Create
            }

            migrationSetup.tables.add(migrationAddedSetupTable)
        }

        // Compare existing tables
        newSetup.tables.forEach { newTable ->
            // Getting corresponding table in old setup
            val oldTable = oldSetup.tables[newTable.tableName, newTable.schemaName]

            // We do not found the old setup table, we can conclude this "newTable" is a new table included in the new setup
            // And therefore will be setup during the last call the EnsureSchema()
            if (oldTable == null)
                return@forEach

            // SyncDirection has no impact if different form old and new setup table.

            val migrationSetupTable = MigrationSetupTable(newTable)

            // Then compare all columns
            if (oldTable.columns.size != newTable.columns.size || !oldTable.columns.all { item1 -> newTable.columns.any { item2 -> item1.equals(item2, true)} })
            {
                migrationSetupTable.storedProcedures = MigrationAction.Create
                migrationSetupTable.trackingTable = MigrationAction.None
                migrationSetupTable.triggers = MigrationAction.Create
                migrationSetupTable.table = MigrationAction.Alter
            }
            else
            {
                migrationSetupTable.storedProcedures = migrationSetup.allStoredProcedures
                migrationSetupTable.trackingTable = migrationSetup.allTrackingTables
                migrationSetupTable.triggers = migrationSetup.allTriggers
                migrationSetupTable.table = MigrationAction.None
            }

            if (migrationSetupTable.shouldMigrate)
                migrationSetup.tables.add(migrationSetupTable)
        }


        // Search for deleted filters
        // TODO : what's the problem if we still have filters, even if not existing ?

        // Search for new filters
        // If we have any filter, just recreate them, just in case
        if (newSetup.filters != null && newSetup.filters.size > 0)
        {
            newSetup.filters.forEach { filter ->
                val setupTable = newSetup.tables[filter.tableName!!, filter.schemaName]

                if (setupTable == null)
                    return@forEach

                var migrationTable = migrationSetup.tables.firstOrNull { ms -> ms.setupTable.equalsByName(setupTable) }

                if (migrationTable == null) {
                    migrationTable = MigrationSetupTable(setupTable).apply {
                        storedProcedures = MigrationAction.Create
                        table = MigrationAction.None
                        trackingTable = MigrationAction.None
                        triggers = MigrationAction.None
                    }
                    migrationSetup.tables.add(migrationTable)
                }

                migrationTable.storedProcedures = MigrationAction.Create
            }

        }

        return migrationSetup
    }
}