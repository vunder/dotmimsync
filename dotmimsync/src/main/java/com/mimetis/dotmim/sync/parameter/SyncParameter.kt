package com.mimetis.dotmim.sync.parameter

import kotlinx.serialization.*
import com.mimetis.dotmim.sync.PrimitiveSerializer

@Serializable
class SyncParameter(
        @SerialName("pn")
        var name: String,

        @SerialName("v")
        @Serializable(with = PrimitiveSerializer::class)
        var value: Any
)



//object SyncParameterSerializer : KSerializer<SyncParameter> {
//    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SyncParameter") {
//        element("name", serialDescriptor<String>())
//        element("type", serialDescriptor<String>())
//        element("value", buildClassSerialDescriptor("Any"))
//    }
//
//    @Suppress("UNCHECKED_CAST")
//    private val dataTypeSerializers: Map<String, KSerializer<Any>> =
//            mapOf(
//                    "String" to serializer<String>(),
//                    "Int" to serializer<Int>(),
//                    "Boolean" to serializer<Boolean>(),
//                    //list them all
//            ).mapValues { (_, v) -> v as KSerializer<Any> }
//
//    private fun getPayloadSerializer(dataType: String): KSerializer<Any> = dataTypeSerializers[dataType]
//            ?: throw SerializationException("Serializer for class $dataType is not registered in PacketSerializer")
//
//    override fun serialize(encoder: Encoder, value: SyncParameter) {
//        encoder.encodeStructure(descriptor) {
//            encodeStringElement(descriptor, 0, value.name)
//            encodeStringElement(descriptor, 1, value.value.javaClass.simpleName)
//            encodeSerializableElement(descriptor, 2, getPayloadSerializer(value.value.javaClass.simpleName), value.value)
//        }
//    }
//
//    @ExperimentalSerializationApi
//    override fun deserialize(decoder: Decoder): SyncParameter = decoder.decodeStructure(descriptor) {
//        decoder.if (decodeSequentially()) {
//            val name = decodeStringElement(descriptor, 0)
//            val value = decodeSerializableElement(descriptor, 1, getPayloadSerializer(dataType))
//            Packet(dataType, payload)
//        } else {
//            require(decodeElementIndex(descriptor) == 0) { "dataType field should precede payload field" }
//            val dataType = decodeStringElement(descriptor, 0)
//            val payload = when (val index = decodeElementIndex(descriptor)) {
//                1 -> decodeSerializableElement(descriptor, 1, getPayloadSerializer(dataType))
//                CompositeDecoder.DECODE_DONE -> throw SerializationException("payload field is missing")
//                else -> error("Unexpected index: $index")
//            }
//            Packet(dataType, payload)
//        }
//    }
//}
