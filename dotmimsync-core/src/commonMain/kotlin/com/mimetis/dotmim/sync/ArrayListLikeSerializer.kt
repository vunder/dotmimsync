package com.mimetis.dotmim.sync

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import com.mimetis.dotmim.sync.parameter.SyncParameter
import java.lang.reflect.ParameterizedType

open class ArrayListLikeSerializer<T, R>(private val elementSerializer: KSerializer<R>) : KSerializer<T>
        where T : ArrayList<R> {
    override val descriptor: SerialDescriptor = listSerialDescriptor<SyncParameter>()

    override fun serialize(encoder: Encoder, value: T) {
        val size = value.size
        val composite = encoder.beginCollection(descriptor, size)
        for (index in 0 until size)
            composite.encodeSerializableElement(elementSerializer.descriptor, index, elementSerializer, value[index])
        composite.endStructure(descriptor)
    }

    inline fun<reified X > foo(): KSerializer<R> {
        return serializer(X::class.java) as KSerializer<R>
    }

    override fun deserialize(decoder: Decoder): T {
        val parameters = ((this.javaClass
                .genericSuperclass as ParameterizedType)
                .actualTypeArguments[0] as Class<T>).constructors.elementAt(0).newInstance() as T

        val compositeDecoder = decoder.beginStructure(descriptor)

        if (compositeDecoder.decodeSequentially()) {
            val size = compositeDecoder.decodeCollectionSize(descriptor)
            for (i in 0 until size) {
                val element = compositeDecoder.decodeSerializableElement(elementSerializer.descriptor, i, elementSerializer)
                parameters.add(element)
            }
        } else {
            while (true) {
                val index = compositeDecoder.decodeElementIndex(elementSerializer.descriptor)
                if (index == CompositeDecoder.DECODE_DONE) break
                val element = compositeDecoder.decodeSerializableElement(elementSerializer.descriptor, index, elementSerializer)
                parameters.add(element)
            }
        }
        compositeDecoder.endStructure(descriptor)

        return parameters
    }
}
