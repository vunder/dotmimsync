package com.mimetis.dotmim.sync.messages

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.DataRowState

/**
 * Summary of table changes applied on a source
 */
@Serializable
class TableChangesApplied(
        /**
         * Gets or sets the name of the table that the DmTableSurrogate object represents.
         */
        @SerialName("tn")
        var tableName: String,

        /**
         * Get or Set the schema used for the DmTableSurrogate
         */
        @SerialName("sn")
        var schemaName: String? = null,

        /**
         * Gets the RowState of the applied rows
         */
        @SerialName("st")
        var state: DataRowState,

        /**
         * Gets the resolved conflict rows applied count
         */
        @SerialName("rc")
        var resolvedConflicts: Int,

        /**
         * Gets the rows changes applied count. This count contains resolved conflicts count also
         */
        @SerialName("a")
        var applied: Int,

        /**
         * Gets the rows changes failed count
         */
        @SerialName("f")
        var failed: Int,

        /**
         * Gets the total rows count to apply for all tables
         */
        @SerialName("trc")
        var totalRowsCount: Int = 0,

        /**
         * Gets the total rows count applied on all tables
         */
        @SerialName("tac")
        var totalAppliedCount: Int = 0
)
