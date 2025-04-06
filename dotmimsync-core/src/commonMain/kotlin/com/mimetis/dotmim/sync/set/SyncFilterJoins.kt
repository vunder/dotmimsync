package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SyncFilterJoinsSerializer::class)
class SyncFilterJoins() : CustomList<SyncFilterJoin>() {

    internal constructor(items: List<SyncFilterJoin>) : this() {
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

        this.forEach { it.ensureFilterJoin(schema) }
    }
}

@OptIn(ExperimentalSerializationApi::class)
object SyncFilterJoinsSerializer : ArrayListLikeSerializer<SyncFilterJoins, SyncFilterJoin>(SyncFilterJoin.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SyncFilterJoin>()

    override fun deserialize(decoder: Decoder): SyncFilterJoins {
        val items = ArrayList<SyncFilterJoin>()

        val compositeDecoder = decoder.beginStructure(descriptor)

        if (compositeDecoder.decodeSequentially()) {
            val size = compositeDecoder.decodeCollectionSize(descriptor)
            for (i in 0 until size) {
                val element = compositeDecoder.decodeSerializableElement(
                    SyncFilterJoinsSerializer.elementSerializer.descriptor,
                    i,
                    SyncFilterJoinsSerializer.elementSerializer
                )
                items.add(element)
            }
        } else {
            while (true) {
                val index = compositeDecoder.decodeElementIndex(SyncFilterJoinsSerializer.elementSerializer.descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                val element = compositeDecoder.decodeSerializableElement(
                    SyncFilterJoinsSerializer.elementSerializer.descriptor,
                    index,
                    SyncFilterJoinsSerializer.elementSerializer
                )
                items.add(element)
            }
        }
        compositeDecoder.endStructure(descriptor)

        return SyncFilterJoins(items)
    }
}
