package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import com.mimetis.dotmim.sync.DataRowState
import com.mimetis.dotmim.sync.SyncTypeConverter

@Serializable(with = SyncRowsSerializer::class)
class SyncRows() : ArrayList<SyncRow>() {
    @Transient
    var table: SyncTable? = null

    constructor(table: SyncTable) : this() {
        this.table = table
    }

    fun ensureRows(table: SyncTable) {
        this.table = table
        this.forEach { it.table = table }
    }

    /**
     * Import a containerTable
     */
    fun importContainerTable(containerTable: ContainerTable, checkType: Boolean) {
        for (row in containerTable.rows!!) {
            val length = table!!.columns!!.size
            val itemArray = Array<Any?>(length) {}

            if (!checkType) {
                System.arraycopy(row, 1, itemArray, 0, length)
            } else {
                // Get only writable columns
                val columns = table!!.getMutableColumnsWithPrimaryKeys()

                for (col in columns) {
                    val value = row[col.ordinal + 1]
                    val colDataType = col.getDataType()

                    if (value == null)
                        itemArray[col.ordinal] = null
                    else if (value::class.java != colDataType)
                        itemArray[col.ordinal] =
                            SyncTypeConverter.tryConvertTo(value, col.getDataType())
                    else
                        itemArray[col.ordinal] = value
                }
            }

            //Array.Copy(row, 1, itemArray, 0, length);
            val state = DataRowState.values().first { it.value == row[0] as Int }

            val schemaRow = SyncRow(this.table!!, itemArray, state)
            this.add(schemaRow)
        }
    }

    /**
     * Gets the inner rows for serialization
     */
    fun exportToContainerTable(): List<Array<Any?>> =
        this.map { row -> row.toArray() }

    override fun clear() {
        this.forEach { it.clear() }
        super.clear()
    }
}

object SyncRowsSerializer : ArrayListLikeSerializer<SyncRows, SyncRow>(SyncRow.serializer())
