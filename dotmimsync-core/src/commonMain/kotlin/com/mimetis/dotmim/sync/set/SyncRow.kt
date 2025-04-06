package com.mimetis.dotmim.sync.set

import com.mimetis.dotmim.sync.DataRowState
import com.mimetis.dotmim.sync.PrimitiveSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class SyncRow(@Transient val length: Int = 0) {
    private var buffer: Array<@Serializable(with = PrimitiveSerializer::class) Any?> =
        Array(length) {}

    lateinit var table: SyncTable
    lateinit var rowState: DataRowState

    //    val length: Int
//        get() = buffer.size

    constructor(
        table: SyncTable,
        row: Array<Any?>,
        state: DataRowState = DataRowState.Unchanged
    ) : this(row.size) {
        buffer = row
        this.table = table
        this.rowState = state
    }

    operator fun get(columnName: String): Any? {
        val index = table.columns!!.getIndex(columnName)
        return this[index]
    }

    /**
     * Get the value in the array that correspond to the SchemaColumn instance given
     */
    operator fun get(column: SyncColumn): Any? =
        this[column.columnName]

    operator fun set(columnName: String, value: Any?) {
        val column = table.columns!![columnName]
        val index = table.columns!!.indexOf(column)
        this[index] = value
    }

    operator fun get(index: Int): Any? {
        return buffer[index]
    }

    operator fun set(index: Int, value: Any?) {
        buffer[index] = value
    }

    fun toArray(): Array<Any?> {
        val array = Array<Any?>(this.length + 1) {}
        buffer.copyInto(array, 1, 0)
        array[0] = this.rowState.value
        return array
    }

    /**
     * Import a raw array, containing state on Index 0
     */
    fun fromArray(row: Array<Any?>) {
        val length = table.columns?.size ?: 0

        if (row.size != length + 1)
            throw Exception("row must contains State on position 0 and UpdateScopeId on position 1")

        row.copyInto(buffer, 0, 1)
        val drs = row[0]
        this.rowState = if (drs is DataRowState) {
            drs
        } else {
            DataRowState.values().first { it.value == drs }
        }
    }

    fun clear() {
        buffer.fill(null)
        //this.table = null
    }
}
