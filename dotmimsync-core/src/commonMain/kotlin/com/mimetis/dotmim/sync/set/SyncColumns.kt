package com.mimetis.dotmim.sync.set

import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SyncColumnsSerializer::class)
class SyncColumns() : CustomList<SyncColumn>() {
    private val columnsDictionary = HashMap<String, SyncColumn?>()
    private val columnsIndexDictionary = HashMap<String, Int>()

    /**
     * Column's schema
     */
    @Transient
    var table: SyncTable? = null

    internal constructor(items: List<SyncColumn>) : this() {
        internalList.addAll(items)
    }

    constructor(table: SyncTable) : this() {
        this.table = table
    }

    fun ensureColumns(table: SyncTable) {
        this.table = table
    }

    fun getIndex(columnName: String): Int =
        columnsIndexDictionary.getOrPut(columnName) {
            internalList.indexOfFirst { c ->
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
            internalList.firstOrNull { c ->
                c.columnName.equals(
                    columnName,
                    true
                )
            }
        }
}

@OptIn(ExperimentalSerializationApi::class)
object SyncColumnsSerializer :
    ArrayListLikeSerializer<SyncColumns, SyncColumn>(SyncColumn.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SyncColumn>()

    override fun deserialize(decoder: Decoder): SyncColumns {
        val items = ArrayList<SyncColumn>()

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
                val index = compositeDecoder.decodeElementIndex(elementSerializer.descriptor)
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

        return SyncColumns(items)
    }
}
