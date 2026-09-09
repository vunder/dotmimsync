package com.mimetis.dotmim.sync

class MissingTableException(tableName: String) : Exception("Table $tableName does not exists") {
}
