package com.mimetis.dotmim.sync

import java.lang.Exception

class MissingTableException(tableName: String) : Exception("Table $tableName does not exists") {
}
