package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.SyncVersion
import com.mimetis.dotmim.sync.set.compareWith

@Serializable
class SyncSetup(
        /**
         * Gets or Sets the tables involved in the sync
         */
        @SerialName("tbls")
        var tables: SetupTables = SetupTables(),

        /**
         * Gets or Sets the filters involved in the sync
         */
        @SerialName("fils")
        var filters: SetupFilters = SetupFilters(),

        /**
         * Specify a prefix for naming stored procedure. Default is empty string
         */
        @SerialName("spp")
        var storedProceduresPrefix: String = "",

        /**
         * Specify a suffix for naming stored procedures. Default is empty string
         */
        @SerialName("sps")
        var storedProceduresSuffix: String = "",

        /**
         *  Specify a prefix for naming stored procedure. Default is empty string
         */
        @SerialName("tf")
        var triggersPrefix: String = "",

        /**
         * Specify a suffix for naming stored procedures. Default is empty string
         */
        @SerialName("ts")
        var triggersSuffix: String = "",

        /**
         * Specify a prefix for naming tracking tables. Default is empty string
         */
        @SerialName("ttp")
        var trackingTablesPrefix: String = "",

        /**
         * Specify a suffix for naming tracking tables.
         */
        @SerialName("tts")
        var trackingTablesSuffix: String = "",

        /**
         * Gets or Sets the current Setup version.
         */
        @SerialName("v")
        var version: String = SyncVersion.current
) {
    /**
     * Check if Setup has tables
     */
    @Transient
    val hasTables: Boolean
        get() = this.tables.size > 0

    /**
     * Check if Setup has a table that has columns
     */
    fun hasTableWithColumns(tableName: String) = (this.tables[tableName]?.columns?.size ?: 0) > 0

    /**
     * Check if two setups have the same local options
     */
    fun hasSameOptions(otherSetup: SyncSetup?) =
            otherSetup != null
                    && this.storedProceduresPrefix.equals(otherSetup.storedProceduresPrefix, true)
                    && this.storedProceduresSuffix.equals(otherSetup.storedProceduresSuffix, true)
                    && this.trackingTablesPrefix.equals(otherSetup.trackingTablesPrefix, true)
                    && this.trackingTablesSuffix.equals(otherSetup.trackingTablesSuffix, true)
                    && this.triggersPrefix.equals(otherSetup.triggersPrefix, true)
                    && this.triggersSuffix.equals(otherSetup.triggersSuffix, true)

    /**
     * Check if two setups have the same tables / filters structure
     */
    fun hasSameStructure(otherSetup: SyncSetup?) =
            otherSetup != null
                    && this.tables.compareWith(otherSetup.tables)
                    && this.filters.compareWith(otherSetup.filters)

    fun equalsByProperties(otherSetup: SyncSetup?): Boolean {
        if (otherSetup == null)
            return false

        if (!hasSameOptions(otherSetup))
            return false

        if (!hasSameStructure(otherSetup))
            return false

        return true
    }
}
