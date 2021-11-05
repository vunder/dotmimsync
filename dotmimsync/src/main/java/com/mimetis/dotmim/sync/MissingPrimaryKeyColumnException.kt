package com.mimetis.dotmim.sync

class MissingPrimaryKeyColumnException(columnName: String, sourceTableName: String) : Exception("Primary key column $columnName should be part of the columns list in your Setup table $sourceTableName.") {
}
