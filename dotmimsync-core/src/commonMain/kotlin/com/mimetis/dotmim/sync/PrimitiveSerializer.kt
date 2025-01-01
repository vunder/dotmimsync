package com.mimetis.dotmim.sync

import android.util.Base64
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@ExperimentalSerializationApi
object PrimitiveSerializer : KSerializer<Any> {
    private val dateFormat = LocalDateTime.Format { byUnicodePattern("yyyy-MM-dd'T'HH:mm:ss") }
//    private const val dateFormat = "yyyy-MM-dd'T'HH:mm:ss"

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("com.mimetis.dotmim.sync.PrimitiveSerializer", PolymorphicKind.SEALED)

    override fun serialize(encoder: Encoder, value: Any) {
        when (value) {
            is String -> encoder.encodeString(value)
            is Boolean -> encoder.encodeBoolean(value)
            is Int -> encoder.encodeInt(value)
            is Long -> encoder.encodeLong(value)
            is Double -> encoder.encodeDouble(value)
            is Float -> encoder.encodeFloat(value)
            is ByteArray -> encoder.encodeString(Base64.encodeToString(value, Base64.NO_WRAP))
            is Uuid -> encoder.encodeString(value.toString().uppercase())
            is LocalDateTime -> encoder.encodeString(value.format(dateFormat))
            else -> encoder.encodeString(value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement()

        return when {
            element.jsonPrimitive.booleanOrNull != null -> element.jsonPrimitive.boolean
            element.jsonPrimitive.isStringWithNumber() -> element.jsonPrimitive.content
            element.jsonPrimitive.intOrNull != null -> element.jsonPrimitive.int
            element.jsonPrimitive.longOrNull != null -> element.jsonPrimitive.long
            element.jsonPrimitive.doubleOrNull != null -> element.jsonPrimitive.double
            element.jsonPrimitive.floatOrNull != null -> element.jsonPrimitive.float
            else -> element.jsonPrimitive.content
        }
    }

    private fun JsonPrimitive.isStringWithNumber(): Boolean {
        return this.intOrNull?.let {
            this.content.substringAfter(it.toString()).isNotEmpty()
        } ?: false
    }
}
