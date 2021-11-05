package com.mimetis.dotmim.sync.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class DatabaseMetadatasCleaned(
        /**
         * Get the changes selected to be applied for a current table
         */
        @SerialName("tcs")
        val tables: MutableCollection<TableMetadatasCleaned> = ArrayList(),

        /**
         * Gets or Sets the last timestamp used as the limit to clean the table metadatas. All rows below this limit have beed cleaned.
         */
        @SerialName("ttl")
        var timestampLimit: Long = 0
) {
    /**
     * Gets the total number of rows cleaned
     */
    @Transient
    val rowsCleanedCount: Int
        get() = this.tables.sumOf { tcs -> tcs.rowsCleanedCount }

    override fun toString(): String =
            "${this.rowsCleanedCount} rows cleaned."
}
