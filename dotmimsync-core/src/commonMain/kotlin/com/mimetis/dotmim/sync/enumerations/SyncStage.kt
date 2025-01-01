package com.mimetis.dotmim.sync.enumerations

import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.data.EnumByValueSerializer
import com.mimetis.dotmim.sync.data.EnumWithValue

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

class SyncStageSerializer : EnumByValueSerializer<SyncStage>()
