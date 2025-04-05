package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import com.mimetis.dotmim.sync.set.CustomList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SetupFiltersSerializer::class)
class SetupFilters() : CustomList<SetupFilter>() {
    internal constructor(items: List<SetupFilter>) : this() {
        internalList.addAll(items)
    }
}

@OptIn(ExperimentalSerializationApi::class)
object SetupFiltersSerializer : ArrayListLikeSerializer<SetupFilters, SetupFilter>(SetupFilter.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SetupFilter>()

    override fun deserialize(decoder: Decoder): SetupFilters {
        val items = ArrayList<SetupFilter>()

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

        return SetupFilters(items)
    }
}
