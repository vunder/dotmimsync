package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.set.SyncNamedItem

@Serializable
class SetupFilterJoin(
        @SerialName("je")
        var joinEnum: Join,

        @SerialName("tn")
        var tableName: String,

        @SerialName("ltn")
        var leftTableName: String,

        @SerialName("lcn")
        var leftColumnName: String,

        @SerialName("rtn")
        var rightTableName: String,

        @SerialName("rcn")
        var rightColumnName: String
) : SyncNamedItem<SetupFilterJoin>() {
    override fun getAllNamesProperties(): List<String> =
            listOf(
                    joinEnum.toString(),
                    tableName,
                    leftColumnName,
                    leftTableName,
                    rightColumnName,
                    rightTableName
            )
}
