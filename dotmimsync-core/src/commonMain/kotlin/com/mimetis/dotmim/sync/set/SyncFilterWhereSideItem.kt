package com.mimetis.dotmim.sync.set

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class SyncFilterWhereSideItem(
        @SerialName("c")
        var columnName: String,

        @SerialName("t")
        var tableName: String,

        @SerialName("s")
        var schemaName: String = "",

        @SerialName("p")
        var parameterName: String
) : SyncNamedItem<SyncFilterWhereSideItem>() {
    /**
     * Gets the ShemaTable's SyncSchema
     */
    @Transient
    var schema: SyncSet? = null

    /**
     * Ensure filter parameter as the correct schema (since the property is not serialized)
     */
    fun ensureFilterWhereSideItem(schema: SyncSet) {
        this.schema = schema
    }

    override fun getAllNamesProperties(): List<String> =
            listOf(
                    tableName,
                    schemaName,
                    columnName,
                    parameterName
            )
}
