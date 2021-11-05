package com.mimetis.dotmim.sync.set

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.setup.Join

@Serializable
class SyncFilterJoin(
        @SerialName("je")
        var joinEnum: Join,

        @SerialName("tbl")
        var tableName: String,

        @SerialName("ltbl")
        var leftTableName: String,

        @SerialName("lcol")
        var leftColumnName: String,

        @SerialName("rtbl")
        var rightTableName: String,

        @SerialName("rcol")
        var rightColumnName: String
) : SyncNamedItem<SyncFilterJoin>() {
    /**
     * Gets the ShemaTable's SyncSchema
     */
    @Transient
    var schema: SyncSet? = null

    /**
     * Ensure filter parameter as the correct schema (since the property is not serialized)
     */
    fun ensureFilterJoin(schema: SyncSet) {
        this.schema = schema
    }

    override fun getAllNamesProperties(): List<String> =
            listOf(
                    this.joinEnum.toString(),
                    tableName,
                    leftColumnName,
                    leftTableName,
                    rightColumnName,
                    rightTableName
            )
}
