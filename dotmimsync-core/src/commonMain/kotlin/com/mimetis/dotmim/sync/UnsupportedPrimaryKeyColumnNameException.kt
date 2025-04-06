package com.mimetis.dotmim.sync

class UnsupportedPrimaryKeyColumnNameException(
    columnName: String,
    columnType: String,
    provider: String
) : Exception("The Column name $columnName is not allowed as a primary key. Please consider to change the column name or choose another primary key for your table.")
