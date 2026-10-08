package com.mimetis.dotmim.sync

class MissingPrimaryKeyException(tableName: String) : Exception("Table $tableName does not have any primary key.")
