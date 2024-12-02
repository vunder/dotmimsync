package com.mimetis.dotmim.sync.builders

import com.mimetis.dotmim.sync.set.SyncColumn
import com.mimetis.dotmim.sync.set.SyncTable

abstract class DbBuilder {
    abstract fun ensureDatabase()
    abstract fun ensureTable(tableName: String, schemaName: String): SyncTable
}
