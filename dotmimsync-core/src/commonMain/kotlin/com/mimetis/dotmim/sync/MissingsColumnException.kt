package com.mimetis.dotmim.sync

class MissingsColumnException(sourceTableName: String) : Exception("Table $sourceTableName has no columns.") {
}
