package com.mimetis.dotmim.sync.batch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.set.SyncNamedItem

@Serializable
class BatchPartTableInfo(
        /**
         * Gets or sets the name of the table that the DmTableSurrogate object represents.
         */
        @SerialName("n")
        var tableName: String = "",

        /**
         * Get or Set the schema used for the DmTableSurrogate
         */
        @SerialName("s")
        var schemaName: String? = null,

        /**
         * Tables contained rows count
         */
        @SerialName("rc")
        var rowsCount: Int = 0
) : SyncNamedItem<BatchPartTableInfo>() {
    override fun getAllNamesProperties(): List<String> =
            listOf(tableName, schemaName ?: "")
}
