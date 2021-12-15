package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer

@Serializable(with = SyncRowsSerializer::class)
class SyncRows() : ArrayList<SyncRow>() {
    @Transient
    var table: SyncTable? = null

    constructor(table: SyncTable) : this() {
        this.table = table
    }

    fun ensureRows(table: SyncTable) {
        this.table = table
        this.forEach { it.schemaTable = table }
    }

    override fun add(element: SyncRow): Boolean {
        element.schemaTable = this.table!!
        return super.add(element)
    }

    /**
     * Add a new buffer row. Be careful, row should include state in first index
     */
    fun add(row: Array<Any?>) {
        val schemaRow = SyncRow(this.table!!, row)
        this.add(schemaRow)
    }

    override fun clear() {
        this.forEach { it.clear() }
        super.clear()
    }
}

object SyncRowsSerializer : ArrayListLikeSerializer<SyncRows, SyncRow>(SyncRow.serializer())
