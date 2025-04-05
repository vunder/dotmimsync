package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SyncFilterWhereSideItemsSerializer::class)
class SyncFilterWhereSideItems() : CustomList<SyncFilterWhereSideItem>() {
    internal constructor(items: List<SyncFilterWhereSideItem>) : this() {
        internalList.addAll(items)
    }

    /**
     * Filter's schema
     */
    @Transient
    var schema: SyncSet? = null

    /**
     * Since we don't serializer the reference to the schema, this method will reaffect the correct schema
     */
    fun ensureFilters(schema: SyncSet) {
        this.schema = schema

        this.forEach { it.ensureFilterWhereSideItem(schema) }
    }
}

@OptIn(ExperimentalSerializationApi::class)
object SyncFilterWhereSideItemsSerializer :
    ArrayListLikeSerializer<SyncFilterWhereSideItems, SyncFilterWhereSideItem>(
        SyncFilterWhereSideItem.serializer()
    ) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SyncColumn>()

    override fun deserialize(decoder: Decoder): SyncFilterWhereSideItems {
        val items = ArrayList<SyncFilterWhereSideItem>()

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

        return SyncFilterWhereSideItems(items)
    }
}
