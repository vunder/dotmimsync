package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.set.SyncNamedItem

@Serializable
class SetupFilterWhere(
        @SerialName("tn")
        var tableName: String,

        @SerialName("sn")
        var schemaName: String = "",

        @SerialName("cn")
        var columnName: String,

        @SerialName("pn")
        var parameterName: String
) : SyncNamedItem<SetupFilterWhere>() {
    override fun getAllNamesProperties(): List<String> =
            listOf(tableName, schemaName, columnName, parameterName)
}
