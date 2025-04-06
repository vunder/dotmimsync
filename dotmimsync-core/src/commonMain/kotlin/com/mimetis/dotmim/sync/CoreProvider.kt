package com.mimetis.dotmim.sync

import com.mimetis.dotmim.sync.builders.*
import com.mimetis.dotmim.sync.set.SyncTable
import com.mimetis.dotmim.sync.setup.SyncSetup

abstract class CoreProvider {
    abstract val supportBulkOperations: Boolean
    abstract fun getScopeBuilder(scopeInfoTableName: String): DbScopeBuilder
    abstract fun getDatabaseBuilder(): DbBuilder
    abstract fun getTableBuilder(tableDescription: SyncTable, setup: SyncSetup): DbTableBuilder
    abstract fun getProviderTypeName(): String
    abstract fun getMetadata(): DbMetadata
    abstract fun getSyncAdapter(tableDescription: SyncTable, setup: SyncSetup): DbSyncAdapter

    /**
     * Get naming tables
     */
    abstract fun getParsers(tableDescription: SyncTable, setup: SyncSetup): Pair<ParserName, ParserName>
}
