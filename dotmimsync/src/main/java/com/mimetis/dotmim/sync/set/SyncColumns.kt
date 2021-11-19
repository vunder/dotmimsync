package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer

@Serializable(with = SyncColumnsSerializer::class)
class SyncColumns() : ArrayList<SyncColumn>() {
    private val columnsDictionary = HashMap<String, SyncColumn?>()
    private val columnsIndexDictionary = HashMap<String, Int>()

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

    fun getIndex(columnName: String): Int =
        columnsIndexDictionary.getOrPut(columnName) {
            this.indexOfFirst { c ->
                c.columnName.equals(
                    columnName,
                    true
                )
            }
        }

    /**
     * Get a Column by its name
     */
    operator fun get(columnName: String): SyncColumn? =
        columnsDictionary.getOrPut(columnName) {
            this.firstOrNull { c ->
                c.columnName.equals(
                    columnName,
                    true
                )
            }
        }
//            this.firstOrNull { c -> c.columnName.equals(columnName, true) }

    override fun clear() {
        //this.forEach { it.clear() }
        super.clear()
    }
}

object SyncColumnsSerializer :
    ArrayListLikeSerializer<SyncColumns, SyncColumn>(SyncColumn.serializer())
