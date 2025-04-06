package com.mimetis.dotmim.sync.enumerations

import com.mimetis.dotmim.sync.data.EnumWithValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SyncStageSerializer::class)
enum class SyncStage(override val value: Int) : EnumWithValue {
    None(0),

    BeginSession(1),
    EndSession(2),

    ScopeLoading(3),
    ScopeWriting(4),

    SnapshotCreating(5),
    SnapshotApplying(6),

    SchemaReading(7),

    Provisioning(8),
    Deprovisioning(9),

    ChangesSelecting(10),
    ChangesApplying(11),

    Migrating(12),

    MetadataCleaning(13),
}

class SyncStageSerializer : KSerializer<SyncStage> {
    private var values: Map<Int, SyncStage>? = null

    override val descriptor = PrimitiveSerialDescriptor("com.mimetis.dotmim.sync.EnumByValue.SyncStage", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: SyncStage) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): SyncStage =
        (values
            ?: SyncStage.entries.associateBy { it.value }.also { values = it })[decoder.decodeInt()]!!
}
