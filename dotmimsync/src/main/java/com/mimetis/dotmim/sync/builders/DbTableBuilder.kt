package com.mimetis.dotmim.sync.builders

import com.mimetis.dotmim.sync.manager.DbRelationDefinition
import com.mimetis.dotmim.sync.set.SyncColumn
import com.mimetis.dotmim.sync.set.SyncFilter
import com.mimetis.dotmim.sync.set.SyncTable
import com.mimetis.dotmim.sync.setup.SyncSetup

abstract class DbTableBuilder(
        val tableDescription: SyncTable,
        protected val tableName: ParserName,
        protected val trackingTableName: ParserName,
        protected val setup: SyncSetup
) {
    abstract fun existsTable(): Boolean

    /**
     * Gets a columns list from the datastore
     */
    abstract fun getColumns(): List<SyncColumn>

    /**
     * Get all primary keys. If composite, must be ordered
     */
    abstract fun getPrimaryKeys(): List<SyncColumn>

    /**
     * Gets all relations from a current table. If composite, must be ordered
     */
    abstract fun getRelations(): List<DbRelationDefinition>

    abstract fun createTable()

    abstract fun existsTrackingTable(): Boolean

    abstract fun createTrackingTable(): Boolean

    abstract fun existsTrigger(triggerType: DbTriggerType): Boolean

    abstract fun dropTrigger(triggerType: DbTriggerType)

    abstract fun createTrigger(triggerType: DbTriggerType)

    abstract fun existsTriggerCommand(triggerType: DbTriggerType): Boolean

    abstract fun dropTrackingTable()
    abstract fun dropTable()

    abstract fun createSchema()
    abstract fun existsSchema(): Boolean

    abstract fun addColumn(columnName: String)
    abstract fun existsColumn(columnName: String): Boolean

    abstract fun renameTrackingTable(oldTableName: ParserName)

    abstract fun createStoredProcedure(storedProcedureType: DbStoredProcedureType, filter: SyncFilter?)
}
