package com.mimetis.dotmim.sync.set

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.setup.SyncSetup
import java.io.Closeable

@Serializable
class SyncSet(
    /**
     * Gets or Sets the sync set tables
     */
    @SerialName("t")
    var tables: SyncTables,

    /**
     * Gets or Sets an array of every SchemaRelation belong to this Schema
     */
    @SerialName("r")
    var relations: SyncRelations,

    /**
     * Filters applied on tables
     */
    @SerialName("f")
    var filters: SyncFilters
) : Closeable {
    /**
     * Create a new SyncSet, empty
     */
    constructor() : this(SyncTables(), SyncRelations(), SyncFilters()) {
        this.tables.schema = this
        this.relations.schema = this
        this.filters.schema = this
    }

    /**
     * Creates a new SyncSet based on a Sync setup (containing tables)
     */
    constructor(setup: SyncSetup) : this() {
        setup.filters.forEach { filter -> this.filters.add(filter) }

        setup.tables.forEach { setupTable ->
            this.tables.add(
                SyncTable(
                    setupTable.tableName,
                    setupTable.schemaName
                )
            )
        }

        this.ensureSchema()
    }

    val hasTables: Boolean
        get() = tables.isNotEmpty()

    val hasColumns: Boolean
        get() = tables.flatMap { it.columns ?: emptyList() }.isNotEmpty()

    val hasRows: Boolean
        get() = this.hasTables && this.tables.any { t -> t.rows.isNotEmpty() }

    /**
     * Ensure all tables, filters and relations has the correct reference to this schema
     */
    fun ensureSchema() {
        this.tables.ensureTables(this)
        this.relations.ensureRelations(this)
        this.filters.ensureFilters(this)
    }

    /**
     * Clone the SyncSet schema (without data)
     */
    fun clone(includeTables: Boolean = true): SyncSet {
        val clone = SyncSet(
            SyncTables(),
            SyncRelations(),
            SyncFilters()
        )

        if (!includeTables)
            return clone

        clone.filters.addAll(this.filters.map { f -> f.clone() })
        clone.relations.addAll(this.relations.map { r -> r.clone() })
        clone.tables.addAll(this.tables.map { t -> t.clone() })

        // Ensure all elements has the correct ref to its parent
        clone.ensureSchema()

        return clone
    }

    /**
     * Import a container set in a SyncSet instance
     */
    fun importContainerSet(containerSet: ContainerSet, checkType: Boolean) {
        for (table in containerSet.tables) {
            val syncTable = this.tables[table.tableName, table.schemaName ?: ""]

            if (syncTable == null)
                throw NullPointerException("Table ${table.tableName} does not exist in the SyncSet")

            syncTable.rows.importContainerTable(table, checkType)
        }
    }

    /**
     * Get the rows inside a container.
     * ContainerSet is a serialization container for rows
     */
    fun getContainerSet(): ContainerSet {
        val containerSet = ContainerSet()
        for (table in this.tables) {
            val containerTable = ContainerTable(table).apply {
                rows = table.rows.exportToContainerTable().toMutableList()
            }

            if (containerTable.rows?.isNotEmpty() == true)
                containerSet.tables.add(containerTable)
        }

        return containerSet
    }

    fun clear() {
        this.tables.clear()
        this.relations.clear()
        this.filters.clear()
    }

    override fun close() {
        clear()
        this.tables.schema = null
        this.relations.schema = null
        this.filters.schema = null
    }
}
