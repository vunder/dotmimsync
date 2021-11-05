package com.mimetis.dotmim.sync

import android.database.Cursor
import com.mimetis.dotmim.sync.set.SyncRow
import com.mimetis.dotmim.sync.set.SyncSet
import com.mimetis.dotmim.sync.set.SyncTable
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.util.*

abstract class DbSyncAdapter(
    val tableDescription: SyncTable,
    val setup: SyncSetup
) {
    abstract fun getSelectInitializedChangesWithFilters(): Cursor
    abstract fun getSelectInitializedChanges(): Cursor
    abstract fun getSelectChangesWithFilters(lastTimestamp: Long?): Cursor
    abstract fun getSelectChanges(lastTimestamp: Long?): Cursor
    abstract fun getSelectRow(primaryKeyRow: SyncRow): Cursor

    abstract fun enableConstraints()
    abstract fun disableConstraints()
    abstract fun reset()
    abstract fun deleteRow(
        scopeId: UUID?,
        syncTimeStamp: Long?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int

    abstract fun initializeRow(
        scopeId: UUID?,
        syncTimeStamp: Long?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int

    abstract fun updateRow(
        scopeId: UUID?,
        syncTimeStamp: Long?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int

    abstract fun updateMetadata(
        scopeId: UUID?,
        isDeleted: Boolean,
        forceWrite: Boolean,
        row: SyncRow
    ): Int

    abstract fun deleteMetadata(timestamp: Long): Int
    abstract fun updateUntrackedRows(): Int

    /**
     * Get or Set the current step (could be only Added, Modified, Deleted)
     */
    internal var applyType: DataRowState = DataRowState.Detached

    companion object {
        internal const val BATCH_SIZE = 10000

        /**
         * Create a change table with scope columns and tombstone column
         */
        fun createChangesTable(syncTable: SyncTable, owner: SyncSet): SyncTable {
            if (syncTable.schema == null)
                throw Exception("ArgumentException: Schema can't be null when creating a changes table")

            // Create an empty sync table without columns
            val changesTable = SyncTable(
                syncTable.tableName,
                syncTable.schemaName,
                originalProvider = syncTable.originalProvider,
                syncDirection = syncTable.syncDirection
            )

            // Adding primary keys
            changesTable.primaryKeys.addAll(syncTable.primaryKeys)

            // get ordered columns that are mutables and pkeys
            val orderedNames = syncTable.getMutableColumnsWithPrimaryKeys()

            for (c in orderedNames)
                changesTable.columns?.add(c.clone())

            owner.tables.add(changesTable)

            return changesTable
        }
    }
}
