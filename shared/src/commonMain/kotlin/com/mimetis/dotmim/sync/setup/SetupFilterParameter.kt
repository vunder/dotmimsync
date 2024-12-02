package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.set.SyncNamedItem

/**
 * Represents a filter parameters
 * @sample @CustomerID int NULL = 12
 */
@Serializable
class SetupFilterParameter(
        /**
         * Gets or sets the name of the parameter.
         * for SQL, will be named @{ParamterName}
         * for MySql, will be named in_{ParameterName}
         */
        @SerialName("n")
        var name: String,

        /**
         * Gets of Sets the table name if parameter is a column
         */
        @SerialName("tn")
        var tableName: String? = null,

        /**
         * Gets of Sets the table schema name if parameter is a column
         */
        @SerialName("sn")
        var schemaName: String? = null,

        /**
         * Gets or Sets the parameter db type
         */
        @SerialName("dt")
        var dbType: DbType? = null,

        /**
         * Gets or Sets the parameter default value expression.
         * Be careful, must be expresse in data source language
         */
        @SerialName("dv")
        var defaultValue: String? = null,

        /**
         * Gets or Sets if the parameter is default null
         */
        @SerialName("an")
        var allowNull: Boolean = false,

        /**
         * Gets or Sets the parameter max length (if needed)
         */
        @SerialName("ml")
        var maxLength: Int = 0
) : SyncNamedItem<SetupFilterParameter>() {
    override fun getAllNamesProperties(): List<String?> =
            listOf(tableName, schemaName, name)
}
