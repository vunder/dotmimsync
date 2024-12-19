package com.mimetis.dotmim.sync.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.reflect.ParameterizedType

open class EnumByNameSerializer<T> : KSerializer<T>
        where T : Enum<T> {
    private var values: Map<String, T>? = null

    @Suppress("UNCHECKED_CAST")
    private fun getEnumClass(): Class<T> =
        ((this.javaClass
            .genericSuperclass as ParameterizedType)
            .actualTypeArguments[0] as Class<T>)

    override val descriptor = PrimitiveSerialDescriptor("EnumByName_${getEnumClass().name}", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T =
        (values
            ?: getEnumClass().enumConstants.associateBy { it.name }.also { values = it })[decoder.decodeString()]!!

//    private inline fun <reified R : T> getValue(name: String): T =
//            (values
//                    ?: R::class.java.enumConstants.associateBy { it.name }.also { values = it })[name]!!

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(value.name)
    }
}
