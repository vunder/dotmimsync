package com.mimetis.dotmim.sync.set

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SyncColumnIdentifier(
        @SerialName("n")
        var columnName: String,

        @SerialName("t")
        var tableName: String,

        @SerialName("s")
        var schemaName: String
) : SyncNamedItem<SyncColumnIdentifier>() {
    fun clone(): SyncColumnIdentifier =
            SyncColumnIdentifier(
                    this.columnName,
                    this.tableName,
                    this.schemaName
            )

    override fun getAllNamesProperties(): List<String> =
            listOf(tableName, schemaName, columnName)
}
