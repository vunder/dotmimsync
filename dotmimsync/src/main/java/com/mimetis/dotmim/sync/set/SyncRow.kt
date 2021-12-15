package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.DataRowState
import com.mimetis.dotmim.sync.PrimitiveSerializer
import java.lang.StringBuilder
import java.util.*

@Serializable
class SyncRow(@Transient val length: Int = 0) {
    private var buffer: Array<@Serializable(with = PrimitiveSerializer::class) Any?> =
        Array(length + 1) {}

    /**
     * Gets or Sets the row's table
     */
    lateinit var schemaTable: SyncTable

    //    val length: Int
//        get() = buffer.size

    constructor(
        schemaTable: SyncTable,
        state: DataRowState = DataRowState.Unchanged
    ) : this(schemaTable.columns!!.size) {
        // Affect new state
        this.schemaTable = schemaTable
        // Affect new state
        buffer[0] = state.ordinal
    }

    constructor(
        schemaTable: SyncTable,
        row: Array<Any?>
    ) : this(schemaTable.columns!!.size) {
        if (row.size <= schemaTable.columns!!.size)
            throw IllegalArgumentException("row array must have one more item to store state")
        if (row.size > (schemaTable.columns!!.size + 1))
            throw IllegalArgumentException("row array has too many items")

        // Direct set of the buffer
        this.buffer = row
        // set columns count as length
        this.schemaTable = schemaTable
    }

    var rowState: DataRowState
        get() = DataRowState.values()[this.buffer[0] as Int]
        set(value) {
            this.buffer[0] = value.ordinal
        }

    operator fun get(columnName: String): Any? {
        val index = schemaTable.columns!!.getIndex(columnName)
        return this[index]
    }

    /**
     * Get the value in the array that correspond to the SchemaColumn instance given
     */
    operator fun get(column: SyncColumn): Any? =
        this[column.columnName]

    operator fun set(columnName: String, value: Any?) {
        val index = schemaTable.columns!!.getIndex(columnName)
        this[index] = value
    }

    operator fun get(index: Int): Any? {
        return buffer[index+1]
    }

    operator fun set(index: Int, value: Any?) {
        buffer[index+1] = value
    }

    fun toArray(): Array<Any?> = this.buffer

    fun clear() = Arrays.fill(buffer, 0)

    override fun toString(): String {
        if (this.buffer == null || this.length == 0)
            return "empty row"

        if (!::schemaTable.isInitialized)
            return buffer.toString()

        val sb = StringBuilder(100)
        sb.append("[Sync state]:${this.rowState}")

        val columns = if (this.rowState == DataRowState.Deleted)
            this.schemaTable.getPrimaryKeysColumns()
        else
            this.schemaTable.columns!!

        columns.forEach { c ->
            val o = this[c.columnName]
            val os = o?.toString() ?: "<NULL />"

            sb.append(", [${c.columnName}]:${os}")
        }

        return sb.toString()
    }
}
