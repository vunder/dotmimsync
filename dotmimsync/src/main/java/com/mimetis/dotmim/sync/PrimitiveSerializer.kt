package com.mimetis.dotmim.sync

import android.annotation.SuppressLint
import android.util.Base64
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalSerializationApi
object PrimitiveSerializer : KSerializer<Any> {
    private const val dateFormat = "yyyy-MM-dd'T'HH:mm:ss"

    private val gmtTimeZone = TimeZone.getTimeZone("GMT")

    @SuppressLint("SimpleDateFormat")
    private val simpleDateFormat = SimpleDateFormat(dateFormat).apply { timeZone = gmtTimeZone }

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("PrimitiveSerializer", PolymorphicKind.SEALED)

    override fun serialize(encoder: Encoder, value: Any) {
        when (value) {
            is String -> encoder.encodeString(value)
            is Boolean -> encoder.encodeBoolean(value)
            is Int -> encoder.encodeInt(value)
            is Long -> encoder.encodeLong(value)
            is Double -> encoder.encodeDouble(value)
            is Float -> encoder.encodeFloat(value)
            is ByteArray -> encoder.encodeString(Base64.encodeToString(value, Base64.NO_WRAP))
            is UUID -> encoder.encodeString(value.toString().uppercase())
            is Date -> encoder.encodeString(simpleDateFormat.format(value))
            else -> encoder.encodeString(value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement()

        return when {
            element.jsonPrimitive.booleanOrNull != null -> element.jsonPrimitive.boolean
            element.jsonPrimitive.content.isMultiline() -> element.jsonPrimitive.content
            element.jsonPrimitive.intOrNull != null -> element.jsonPrimitive.int
            element.jsonPrimitive.longOrNull != null -> element.jsonPrimitive.long
            element.jsonPrimitive.doubleOrNull != null -> element.jsonPrimitive.double
            element.jsonPrimitive.floatOrNull != null -> element.jsonPrimitive.float
            else -> element.jsonPrimitive.content
        }
    }

    private fun String.isMultiline(): Boolean {
        if (!this.contains('\r') && !this.contains('\n'))
            return false

        return this.substringAfter('\r').trimIndent().isNotEmpty()
                || this.substringAfter('\n').trimIndent().isNotEmpty()
    }
}
