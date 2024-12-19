package com.mimetis.dotmim.sync

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object DateSerializer : KSerializer<LocalDateTime> {
    private val dateFormat =
        LocalDateTime.Format { byUnicodePattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSX") }

    override val descriptor =
        PrimitiveSerialDescriptor("com.mimetis.dotmim.sync.LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime =
        LocalDateTime.parse(decoder.decodeString(), dateFormat)

    override fun serialize(encoder: Encoder, value: LocalDateTime) =
        encoder.encodeString(value.format(dateFormat))
}
