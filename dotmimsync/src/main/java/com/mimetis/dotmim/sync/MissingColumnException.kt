package com.mimetis.dotmim.sync

class MissingColumnException(columnName: String, sourceTableName: String) : Exception("Column $columnName does not exists in the table $sourceTableName.") {
}
