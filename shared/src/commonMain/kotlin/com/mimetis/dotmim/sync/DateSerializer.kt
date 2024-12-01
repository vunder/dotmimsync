package com.mimetis.dotmim.sync

import android.annotation.SuppressLint
import android.os.Build
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.util.*

object DateSerializer : KSerializer<Date> {
    private val dateFormat =
        // SDK 24
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSX"
        else
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSZZZZZ"

    private val gmtTimeZone = TimeZone.getTimeZone("GMT")

    @SuppressLint("SimpleDateFormat")
    private val simpleDateFormat = SimpleDateFormat(dateFormat).apply { timeZone = gmtTimeZone }

    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Date =
        simpleDateFormat.parse(decoder.decodeString())!!

    override fun serialize(encoder: Encoder, value: Date) =
        encoder.encodeString(simpleDateFormat.format(value))
}
