package com.mimetis.dotmim.sync.set

import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SyncRelationsSerializer::class)
class SyncRelations() : CustomList<SyncRelation>() {
    internal constructor(items: List<SyncRelation>) : this() {
        internalList.addAll(items)
    }

    /**
     * Relation's schema
     */
    @Transient
    var schema: SyncSet? = null

    constructor(schema: SyncSet) : this() {
        this.schema = schema
    }

    /**
     * Since we don't serializer the reference to the schema, this method will reaffect the correct schema
     */
    fun ensureRelations(schema: SyncSet) {
        this.schema = schema
        this.forEach { it.ensureRelation(schema) }
    }

    override fun clear() {
        this.forEach { it.clear() }
        super.clear()
    }
}

@OptIn(ExperimentalSerializationApi::class)
object SyncRelationsSerializer : ArrayListLikeSerializer<SyncRelations, SyncRelation>(SyncRelation.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SyncRelation>()

    override fun deserialize(decoder: Decoder): SyncRelations {
        val items = ArrayList<SyncRelation>()

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

        return SyncRelations(items)
    }
}
