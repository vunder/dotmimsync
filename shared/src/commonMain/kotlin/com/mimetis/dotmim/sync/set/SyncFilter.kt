package com.mimetis.dotmim.sync.set

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Design a filter clause on Dmtable
 */
@Serializable
class SyncFilter(
        @SerialName("t")
        var tableName: String?,

        @SerialName("s")
        var schemaName: String = "",

        /**
         * Gets or Sets the parameters list, used as input in the stored procedure
         */
        @SerialName("p")
        var parameters: SyncFilterParameters = SyncFilterParameters(),

        /**
         * Gets or Sets side where filters list
         */
        @SerialName("w")
        var wheres: SyncFilterWhereSideItems = SyncFilterWhereSideItems(),

        /**
         * Gets or Sets side where filters list
         */
        @SerialName("j")
        var joins: SyncFilterJoins = SyncFilterJoins(),

        /**
         * Gets or Sets customs where
         */
        @SerialName("cw")
        var customWheres: List<String> = ArrayList(),

        /**
         * Gets the ShemaFilter's SyncSchema
         */
        @Transient
        var schema: SyncSet? = null
) : SyncNamedItem<SyncFilter>() {

    /**
     * Ensure filter has the correct schema (since the property is not serialized
     */
    fun ensureFilter(schema: SyncSet) {
        this.schema = schema

        this.parameters.ensureFilters(schema);
        this.wheres.ensureFilters(schema);
        this.joins.ensureFilters(schema);
    }

    /**
     * Clone the SyncFilter
     */
    fun clone(): SyncFilter =
            SyncFilter(this.tableName, this.schemaName)

    fun clear() {
        this.schema = null
    }

    override fun getAllNamesProperties(): List<String?> =
            listOf(tableName, schemaName)
}
