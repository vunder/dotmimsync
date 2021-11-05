package com.mimetis.dotmim.sync.sqlite

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import com.mimetis.dotmim.sync.CoreProvider
import com.mimetis.dotmim.sync.DbSyncAdapter
import com.mimetis.dotmim.sync.builders.*
import com.mimetis.dotmim.sync.set.SyncTable
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.io.Closeable

class SqliteSyncProvider(context: Context, helper: SQLiteOpenHelper) : CoreProvider(),
    Closeable {
    private val database = helper.writableDatabase
    private lateinit var metadata: DbMetadata
    override val supportBulkOperations: Boolean
        get() = false

    override fun getScopeBuilder(scopeInfoTableName: String): DbScopeBuilder =
        SqliteScopeBuilder(scopeInfoTableName, database)

    override fun getDatabaseBuilder(): DbBuilder =
        SqliteBuilder()

    override fun getTableBuilder(tableDescription: SyncTable, setup: SyncSetup): DbTableBuilder {
        val pair = getParsers(tableDescription, setup)
        return SqliteTableBuilder(tableDescription, pair.first, pair.second, setup, database)
    }

    override fun getProviderTypeName(): String =
        ProviderType

    override fun getMetadata(): DbMetadata {
        if (!::metadata.isInitialized)
            metadata = SqliteDbMetadata()
        return metadata
    }

    override fun getSyncAdapter(tableDescription: SyncTable, setup: SyncSetup): DbSyncAdapter {
        val (tableName, trackingTable) = getParsers(tableDescription, setup)
        return SqliteSyncAdapter(tableDescription, tableName, trackingTable, setup, database)
    }

    override fun getParsers(
        tableDescription: SyncTable,
        setup: SyncSetup
    ): Pair<ParserName, ParserName> {
        val tableAndPrefixName = tableDescription.tableName
        val originalTableName = ParserName.parse(tableDescription)

        val pref = if (setup.trackingTablesPrefix.isNotBlank()) setup.trackingTablesPrefix else ""
        var suf = if (setup.trackingTablesSuffix.isNotBlank()) setup.trackingTablesSuffix else ""

        if (pref.isBlank() && suf.isBlank())
            suf = "_tracking"

        val trackingTableName = ParserName.parse("$pref$tableAndPrefixName$suf")

        return Pair(originalTableName, trackingTableName)
    }

    companion object {
        val ProviderType: String
            get() = "SqliteSyncProvider, Dotmim.Sync.Sqlite.SqliteSyncProvider"
    }

    override fun close() {
//        database.close()
    }
}
