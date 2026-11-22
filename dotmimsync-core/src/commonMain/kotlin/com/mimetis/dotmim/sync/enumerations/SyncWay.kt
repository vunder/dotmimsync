package com.mimetis.dotmim.sync.enumerations

import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.data.EnumWithValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class SyncWaySerializer : KSerializer<SyncWay> {
    private var values: Map<Int, SyncWay>? = null

    override val descriptor = PrimitiveSerialDescriptor("com.mimetis.dotmim.sync.EnumByValue.SyncWay", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: SyncWay) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): SyncWay =
        (values
            ?: SyncWay.entries.associateBy { it.value }.also { values = it })[decoder.decodeInt()]!!
}

@Serializable(with = SyncWaySerializer::class)
enum class SyncWay(override val value: Int) : EnumWithValue {
    /// <summary>
    /// No sync engaged
    /// </summary>
    None(0),

    /// <summary>
    /// Sync is selecting then downloading changes from server
    /// </summary>
    Download(1),

    /// <summary>
    /// Sync is selecting then uploading changes from client
    /// </summary>
    Upload(2)
}
