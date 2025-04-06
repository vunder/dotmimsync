package com.mimetis.dotmim.sync.web.client

import com.mimetis.dotmim.sync.data.EnumWithValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class HttpStepSerializer : KSerializer<HttpStep> {
    private var values: Map<Int, HttpStep>? = null

    override val descriptor = PrimitiveSerialDescriptor("com.mimetis.dotmim.sync.EnumByValue.HttpStep", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: HttpStep) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): HttpStep =
        (values
            ?: HttpStep.entries.associateBy { it.value }.also { values = it })[decoder.decodeInt()]!!
}

@Serializable(with = HttpStepSerializer::class)
enum class HttpStep(override val value: Int) : EnumWithValue {
    None(0),
    EnsureSchema(1),
    EnsureScopes(2),
    SendChanges(3),
    SendChangesInProgress(4),
    GetChanges(5),
    GetEstimatedChangesCount(6),
    GetMoreChanges(7),
    GetChangesInProgress(8),
    GetSnapshot(9),
    GetSummary(10),
    SendEndDownloadChanges(1)
}
