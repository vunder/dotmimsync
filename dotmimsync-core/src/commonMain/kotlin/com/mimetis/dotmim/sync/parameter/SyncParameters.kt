package com.mimetis.dotmim.sync.parameter

import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import com.mimetis.dotmim.sync.set.CustomList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SyncParametersSerializer::class)
class SyncParameters() : CustomList<SyncParameter>() {
    internal constructor(items: List<SyncParameter>) : this() {
        internalList.addAll(items)
    }
}

@OptIn(ExperimentalSerializationApi::class)
object SyncParametersSerializer : ArrayListLikeSerializer<SyncParameters, SyncParameter>(SyncParameter.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SyncParameter>()

    override fun deserialize(decoder: Decoder): SyncParameters {
        val items = ArrayList<SyncParameter>()

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

        return SyncParameters(items)
    }
}
