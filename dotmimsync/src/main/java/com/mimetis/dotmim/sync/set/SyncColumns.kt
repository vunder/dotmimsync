package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer

@Serializable(with = SyncColumnsSerializer::class)
class SyncColumns() : ArrayList<SyncColumn>() {
    /**
     * Column's schema
     */
    @Transient
    var table: SyncTable? = null

    constructor(table: SyncTable) : this() {
        this.table = table
    }

    fun ensureColumns(table: SyncTable) {
        this.table = table
    }

    /**
     * Get a Column by its name
     */
    operator fun get(columnName: String): SyncColumn? =
            this.firstOrNull { c -> c.columnName.equals(columnName, true) }

    override fun clear() {
        //this.forEach { it.clear() }
        super.clear()
    }
}

object SyncColumnsSerializer : ArrayListLikeSerializer<SyncColumns, SyncColumn>(SyncColumn.serializer())
