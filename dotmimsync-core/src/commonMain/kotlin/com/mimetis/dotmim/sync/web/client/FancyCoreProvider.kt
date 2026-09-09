package com.mimetis.dotmim.sync.web.client

import com.mimetis.dotmim.sync.CoreProvider
import com.mimetis.dotmim.sync.DbSyncAdapter
import com.mimetis.dotmim.sync.builders.*
import com.mimetis.dotmim.sync.set.SyncTable
import com.mimetis.dotmim.sync.setup.SyncSetup

class FancyCoreProvider : CoreProvider() {
    override val supportBulkOperations: Boolean
        get() = TODO("Not yet implemented")

    override fun getScopeBuilder(scopeInfoTableName: String): DbScopeBuilder {
        TODO("Not yet implemented")
    }

    override fun getDatabaseBuilder(): DbBuilder {
        TODO("Not yet implemented")
    }

    override fun getTableBuilder(tableDescription: SyncTable, setup: SyncSetup): DbTableBuilder {
        TODO("Not yet implemented")
    }

    override fun getProviderTypeName(): String {
        TODO("Not yet implemented")
    }

    override fun getMetadata(): DbMetadata {
        TODO("Not yet implemented")
    }

    override fun getSyncAdapter(tableDescription: SyncTable, setup: SyncSetup): DbSyncAdapter {
        TODO("Not yet implemented")
    }

    override fun getParsers(tableDescription: SyncTable, setup: SyncSetup): Pair<ParserName, ParserName> {
        TODO("Not yet implemented")
    }
}
