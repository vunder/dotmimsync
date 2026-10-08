package com.mimetis.dotmim.sync

class UnsupportedColumnTypeException(
        columnName: String,
        columnType: String,
        provider: String
) : Exception("The Column $columnName of type $columnType from provider $provider is not currently supported.") {
}
