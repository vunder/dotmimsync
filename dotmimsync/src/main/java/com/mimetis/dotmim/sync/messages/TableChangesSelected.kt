package com.mimetis.dotmim.sync.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Get changes to be applied (contains Deletes AND Inserts AND Updates)
 */
@Serializable
class TableChangesSelected(
        /**
         * Gets the table name
         */
        @SerialName("n")
        var tableName: String? = null,

        /**
         * Get or Set the schema used for the DmTableSurrogate
         */
        @SerialName("s")
        var schemaName: String? = null,

        /**
         * Gets or sets the number of deletes that should be applied to a table during the synchronization session.
         */
        @SerialName("d")
        var deletes: Int = 0,

        /**
         * Gets or sets the number of updates OR inserts that should be applied to a table during the synchronization session.
         */
        @SerialName("u")
        var upserts: Int = 0
) {
    /**
     * Gets the total number of changes that are applied to a table during the synchronization session.
     */
    @Transient
    val totalChanges
        get() = this.upserts + this.deletes
}
