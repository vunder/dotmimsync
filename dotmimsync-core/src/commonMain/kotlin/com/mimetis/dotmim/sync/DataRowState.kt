package com.mimetis.dotmim.sync

import com.mimetis.dotmim.sync.data.EnumWithValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class DataRowStateSerializer : KSerializer<DataRowState> {
    private var values: Map<Int, DataRowState>? = null

    override val descriptor = PrimitiveSerialDescriptor("com.mimetis.dotmim.sync.EnumByValue.DataRowState", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: DataRowState) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): DataRowState =
        (values
            ?: DataRowState .entries.associateBy { it.value }.also { values = it })[decoder.decodeInt()]!!
}

@Serializable(with = DataRowStateSerializer::class)
enum class DataRowState(override val value: Int) : EnumWithValue {
    Detached(1),
    Unchanged(2),
    Added(4),
    Deleted(8),
    Modified(16)
}
