package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.set.SyncNamedItem

/**
 *  Design a filter clause on Dmtable
 */
@Serializable
class SetupFilter(
        /**
         * Gets or Sets the name of the table where the filter will be applied (and so the _Changes stored proc)
         */
        @SerialName("tn")
        var tableName: String?,

        /**
         * Gets or Sets the schema name of the table where the filter will be applied (and so the _Changes stored proc)
         */
        @SerialName("sn")
        var schemaName: String = "",

        /**
         * Gets the custom joins list, used with custom wheres
         */
        @SerialName("j")
        var joins: List<SetupFilterJoin> = ArrayList(),

        /**
         * Gets the custom joins list, used with custom wheres
         */
        @SerialName("cw")
        var customWheres: List<String> = ArrayList(),

        /**
         * Gets the parameters list, used as input in the stored procedure
         */
        @SerialName("p")
        var parameters: MutableList<SetupFilterParameter> = ArrayList(),

        /**
         * Side where filters list
         */
        @SerialName("w")
        var wheres: MutableList<SetupFilterWhere> = ArrayList()
) : SyncNamedItem<SetupFilter>() {
    override fun getAllNamesProperties(): List<String?> =
            listOf(this.tableName, this.schemaName)
}
