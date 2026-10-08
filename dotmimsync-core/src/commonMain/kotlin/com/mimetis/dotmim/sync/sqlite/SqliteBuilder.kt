package com.mimetis.dotmim.sync.sqlite

import com.mimetis.dotmim.sync.builders.DbBuilder
import com.mimetis.dotmim.sync.set.SyncTable

class SqliteBuilder : DbBuilder() {
    override fun ensureDatabase() {
    }

    override fun ensureTable(tableName: String, schemaName: String): SyncTable =
            SyncTable(tableName)
}
