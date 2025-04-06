package com.mimetis.dotmim.sync

import com.mimetis.dotmim.sync.set.CustomList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Encoder

abstract class ArrayListLikeSerializer<T, R>(protected val elementSerializer: KSerializer<R>) : KSerializer<T>
        where T : CustomList<R> {
//    override val descriptor: SerialDescriptor = listSerialDescriptor<R>()

    override fun serialize(encoder: Encoder, value: T) {
        val size = value.size
        val composite = encoder.beginCollection(descriptor, size)
        for (index in 0 until size)
            composite.encodeSerializableElement(elementSerializer.descriptor, index, elementSerializer, value[index])
        composite.endStructure(descriptor)
    }

//    inline fun<reified X > foo(): KSerializer<R> {
//        return serializer<X>() as KSerializer<R>
//    }

//    override fun deserialize(decoder: Decoder): T {
//        val items = ArrayList<R>()
//        ((this.javaClass
//                .genericSuperclass as ParameterizedType)
//                .actualTypeArguments[0] as Class<T>).constructors.elementAt(0).newInstance() as T
//
//        val compositeDecoder = decoder.beginStructure(descriptor)
//
//        if (compositeDecoder.decodeSequentially()) {
//            val size = compositeDecoder.decodeCollectionSize(descriptor)
//            for (i in 0 until size) {
//                val element = compositeDecoder.decodeSerializableElement(elementSerializer.descriptor, i, elementSerializer)
//                items.add(element)
//            }
//        } else {
//            while (true) {
//                val index = compositeDecoder.decodeElementIndex(elementSerializer.descriptor)
//                if (index == CompositeDecoder.DECODE_DONE) break
//                val element = compositeDecoder.decodeSerializableElement(elementSerializer.descriptor, index, elementSerializer)
//                items.add(element)
//            }
//        }
//        compositeDecoder.endStructure(descriptor)
//
//        return parameters
//    }
}
