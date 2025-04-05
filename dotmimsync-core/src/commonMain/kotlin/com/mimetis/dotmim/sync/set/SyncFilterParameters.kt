package com.mimetis.dotmim.sync.set

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SyncFilterParametersSerializer::class)
class SyncFilterParameters() : CustomList<SyncFilterParameter>() {

    internal constructor(items: List<SyncFilterParameter>) : this() {
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

        this.forEach { it.ensureFilterParameter(schema) }
    }
}

@OptIn(ExperimentalSerializationApi::class)
object SyncFilterParametersSerializer : ArrayListLikeSerializer<SyncFilterParameters, SyncFilterParameter>(SyncFilterParameter.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SyncColumn>()

    override fun deserialize(decoder: Decoder): SyncFilterParameters {
        val items = ArrayList<SyncFilterParameter>()

        val compositeDecoder = decoder.beginStructure(descriptor)

        if (compositeDecoder.decodeSequentially()) {
            val size = compositeDecoder.decodeCollectionSize(descriptor)
            for (i in 0 until size) {
                val element = compositeDecoder.decodeSerializableElement(
                    SyncFilterParametersSerializer.elementSerializer.descriptor,
                    i,
                    SyncFilterParametersSerializer.elementSerializer
                )
                items.add(element)
            }
        } else {
            while (true) {
                val index = compositeDecoder.decodeElementIndex(SyncFilterParametersSerializer.elementSerializer.descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                val element = compositeDecoder.decodeSerializableElement(
                    SyncFilterParametersSerializer.elementSerializer.descriptor,
                    index,
                    SyncFilterParametersSerializer.elementSerializer
                )
                items.add(element)
            }
        }
        compositeDecoder.endStructure(descriptor)

        return SyncFilterParameters(items)
    }
}
