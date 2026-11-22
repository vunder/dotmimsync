package com.mimetis.dotmim.sync.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class TableMetadatasCleaned(
        /**
         * Gets or sets the name of the table that the DmTableSurrogate object represents.
         */
        @SerialName("tn")
        var tableName: String,

        /**
         * Get or Set the schema used for the DmTableSurrogate
         */
        @SerialName("sn")
        var schemaName: String,

        /**
         * Gets or Sets the last timestamp used as the limit to clean the table metadatas. All rows below this limit have beed cleaned.
         */
        @SerialName("ttl")
        var timestampLimit: Long,

        /**
         * Gets or Sets the metadatas rows count, that have been cleaned
         */
        @SerialName("rcc")
        var rowsCleanedCount: Int
)
