package com.mimetis.dotmim.sync.setup

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import com.mimetis.dotmim.sync.ArrayListLikeSerializer
import com.mimetis.dotmim.sync.set.CustomList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

@Serializable(with = SetupColumnsSerializer::class)
class SetupColumns() : CustomList<String>() {
    internal constructor(items: List<String>) : this() {
        internalList.addAll(items)
    }
}

@OptIn(ExperimentalSerializationApi::class)
object SetupColumnsSerializer : ArrayListLikeSerializer<SetupColumns, String>(String.serializer()) {
    override val descriptor: SerialDescriptor = listSerialDescriptor<String>()

    override fun deserialize(decoder: Decoder): SetupColumns {
        val items = ArrayList<String>()

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

        return SetupColumns(items)
    }
}
