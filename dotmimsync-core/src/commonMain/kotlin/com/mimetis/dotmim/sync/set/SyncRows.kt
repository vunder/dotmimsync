package com.mimetis.dotmim.sync.set

import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import com.mimetis.dotmim.sync.DataRowState
import com.mimetis.dotmim.sync.SyncTypeConverter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SyncRowsSerializer::class)
class SyncRows() : CustomList<SyncRow>() {
    @Transient
    var table: SyncTable? = null

    internal constructor(items: List<SyncRow>) : this() {
        internalList.addAll(items)
    }

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
//                System.arraycopy(row, 1, itemArray, 0, length)
                row.copyInto(itemArray, 0, 1, length - 1)
            } else {
                // Get only writable columns
                val columns = table!!.getMutableColumnsWithPrimaryKeys()

                for (col in columns) {
                    val value = row[col.ordinal + 1]
                    val colDataType = col.getDataType()

                    if (value == null)
                        itemArray[col.ordinal] = null
                    else if (value::class != colDataType)
                        itemArray[col.ordinal] =
                            SyncTypeConverter.tryConvertTo(value, col.getDataType())
                    else
                        itemArray[col.ordinal] = value
                }
            }

            //Array.Copy(row, 1, itemArray, 0, length);
            val state = DataRowState.entries.first { it.value == row[0] as Int }

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

@OptIn(ExperimentalSerializationApi::class)
object SyncRowsSerializer : ArrayListLikeSerializer<SyncRows, SyncRow>(SyncRow.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SyncRow>()

    override fun deserialize(decoder: Decoder): SyncRows {
        val items = ArrayList<SyncRow>()

        val compositeDecoder = decoder.beginStructure(descriptor)

        if (compositeDecoder.decodeSequentially()) {
            val size = compositeDecoder.decodeCollectionSize(descriptor)
            for (i in 0 until size) {
                val element = compositeDecoder.decodeSerializableElement(
                    elementSerializer.descriptor,
                    i,
                    elementSerializer
                )
                items.add(element)
            }
        } else {
            while (true) {
                val index =
                    compositeDecoder.decodeElementIndex(elementSerializer.descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                val element = compositeDecoder.decodeSerializableElement(
                    elementSerializer.descriptor,
                    index,
                    elementSerializer
                )
                items.add(element)
            }
        }
        compositeDecoder.endStructure(descriptor)

        return SyncRows(items)
    }
}
