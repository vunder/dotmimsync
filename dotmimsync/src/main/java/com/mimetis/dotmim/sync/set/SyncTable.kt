package com.mimetis.dotmim.sync.set

import android.database.Cursor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.DataRowState
import com.mimetis.dotmim.sync.enumerations.SyncDirection
import com.mimetis.dotmim.sync.sqlite.CursorHelper.getDataType
import com.mimetis.dotmim.sync.sqlite.CursorHelper.getValue

/**
 * Represents a table schema
 */
@Serializable
class SyncTable(
        /**
         * Gets or sets the name of the table that the DmTableSurrogate object represents.
         */
        @SerialName("n")
        var tableName: String,

        /**
         * Get or Set the schema used for the DmTableSurrogate
         */
        @SerialName("s")
        var schemaName: String = "",

        /**
         * Gets or Sets the original provider (SqlServer, MySql, Sqlite, Oracle, PostgreSQL)
         */
        @SerialName("op")
        var originalProvider: String? = null,

        /**
         * Gets or Sets the Sync direction (may be Bidirectional, DownloadOnly, UploadOnly)
         * Default is @see SyncDirection.Bidirectional
         */
        @SerialName("sd")
        var syncDirection: SyncDirection = SyncDirection.Bidirectional,

        /**
         * Gets or Sets the table columns
         */
        @SerialName("c")
        var columns: SyncColumns? = null,

        /**
         * Gets or Sets the table primary keys
         */
        @SerialName("pk")
        var primaryKeys: ArrayList<String> = ArrayList()
) : SyncNamedItem<SyncTable>() {
    /**
     * Gets the ShemaTable's rows
     */
    @Transient
    val rows = SyncRows(this)

    /**
     * Table's schema
     */
    @Transient
    var schema: SyncSet? = null

    /**
     * Gets a value indicating if the synctable has rows
     */
    @Transient
    val hasRows
        get() = this.rows.isNotEmpty()

    constructor(tableName: String) : this(tableName, "")

    /**
     * Since we don't serializer the reference to the schema, this method will reaffect the correct schema
     */
    fun ensureTable(schema: SyncSet) {
        this.schema = schema
        this.columns?.ensureColumns(this)
        this.rows.ensureRows(this)
    }

    fun load(cursor: Cursor, filter: ((cursor: Cursor) -> Boolean)? = null) {
        val readerFieldCount = cursor.columnCount
        if (readerFieldCount == 0 || cursor.count == 0)
            return

        while (cursor.moveToNext()) {
            if (this.columns?.isEmpty() == true) {
                for (i in 0 until readerFieldCount) {
                    val columnName = cursor.getColumnName(i)
                    val columnType = cursor.getType(i).getDataType()
                    this.columns?.add(SyncColumn(columnName, columnType))
                }
            }

            if (filter == null || filter(cursor)) {
                val row = newRow()
                for (i in 0 until readerFieldCount) {
                    val columnName = cursor.getColumnName(i)
                    row[columnName] = cursor.getValue(i)
                }
                this.rows.add(row)
            }
        }
    }

    fun newRow(state: DataRowState = DataRowState.Unchanged): SyncRow {
        val row = SyncRow(this.columns?.size ?: 0).apply {
            rowState = state
            table = this@SyncTable
        }
        return row
    }

    fun getRelations(): List<SyncRelation> {
        if (this.schema == null)
            return emptyList()
        return schema!!.relations.filter { r -> r.getTable()!!.equalsByName(this) }
    }

    /**
     * Gets the full name of the table, based on schema name + "." + table name (if schema name exists)
     */
    fun getFullName(): String =
            if (this.schemaName.isBlank()) this.tableName else "$schemaName.$tableName"

    /**
     * Get all columns that are Primary keys, based on the names we have in PrimariKeys property
     */
    fun getPrimaryKeysColumns(): List<SyncColumn> =
            this.columns!!.sortedBy { c -> c.ordinal }
                    .filter { column ->
                        this.primaryKeys.any { pkey -> column.columnName.equals(pkey, true) }
                    }

    /**
     * Get all columns that can be updated
     */
    fun getMutableColumns(includeAutoIncrement: Boolean = true, includePrimaryKeys: Boolean = false): List<SyncColumn> =
            this.columns!!.sortedBy { c -> c.ordinal }
                    .filter { column -> !column.isCompute && !column.isReadOnly }
                    .filter { column ->
                        val isPrimaryKey = this.primaryKeys.any { pkey -> column.columnName.equals(pkey, true) }
                        return@filter (includePrimaryKeys && isPrimaryKey) || (!isPrimaryKey && (includeAutoIncrement || (!includeAutoIncrement && !column.isAutoIncrement)))
                    }

    /**
     * Get all columns that can be queried
     */
    fun getMutableColumnsWithPrimaryKeys(): List<SyncColumn> =
            this.columns!!
                    .sortedBy { c -> c.ordinal }
                    .filter { column -> !column.isCompute && !column.isReadOnly }

    /**
     * Clone the table structure (without rows)
     */
    fun clone(): SyncTable =
            SyncTable(
                    this.tableName,
                    this.schemaName,
                    this.originalProvider,
                    this.syncDirection,
                    SyncColumns(this).apply { addAll(this@SyncTable.columns!!) },
                    ArrayList(this.primaryKeys)
            )

    fun clear() {
        this.rows.clear()
        this.columns?.clear()
    }

    /**
     * Get all filters for a selected sync table
     */
    fun getFilter(): SyncFilter? {
        if (this.schema == null || this.schema?.filters == null || this.schema!!.filters.isEmpty())
            return null

        return this.schema!!.filters.firstOrNull { sf ->
            val sn = if (sf.schemaName.isNullOrBlank()) "" else sf.schemaName
            val otherSn = if (this.schemaName.isNullOrBlank()) "" else this.schemaName

            return@firstOrNull this.tableName.equals(sf.tableName, true) && sn.equals(otherSn, true)
        }
    }

    override fun getAllNamesProperties(): List<String> =
            listOf(this.tableName, this.schemaName)

    override fun equalsByProperties(otherInstance: SyncTable?): Boolean {
        if (otherInstance == null)
            return false;

        if (!this.equalsByName(otherInstance))
            return false

        // checking properties
        if (this.syncDirection != otherInstance.syncDirection)
            return false;

        if (!this.originalProvider.equals(otherInstance.originalProvider, true))
            return false;

        // Check list
        if (!this.columns.compareWith(otherInstance.columns))
            return false

        if (!this.primaryKeys.compareWith(otherInstance.primaryKeys))
            return false

        return true
    }

    init {
        if (columns == null)
            columns = SyncColumns(this)
    }
}
