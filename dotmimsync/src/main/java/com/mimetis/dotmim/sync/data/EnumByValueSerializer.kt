package com.mimetis.dotmim.sync.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.reflect.ParameterizedType

open class EnumByValueSerializer<T> : KSerializer<T>
        where T : Enum<T>, T : EnumWithValue {
    private var values: Map<Int, T>? = null

    @Suppress("UNCHECKED_CAST")
    private fun getEnumClass(): Class<T> =
        ((this.javaClass
            .genericSuperclass as ParameterizedType)
            .actualTypeArguments[0] as Class<T>)

    override val descriptor = PrimitiveSerialDescriptor("EnumByValue_${getEnumClass().name}", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): T =
        (values
            ?: getEnumClass().enumConstants.associateBy { it.value }.also { values = it })[decoder.decodeInt()]!!

//    private inline fun <reified R : T> getValue(name: String): T =
//            (values
//                    ?: R::class.java.enumConstants.associateBy { it.name }.also { values = it })[name]!!

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeInt(value.value)
    }
}
