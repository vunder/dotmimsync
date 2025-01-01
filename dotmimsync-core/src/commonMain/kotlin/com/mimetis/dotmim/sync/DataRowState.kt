package com.mimetis.dotmim.sync

import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.data.EnumByValueSerializer
import com.mimetis.dotmim.sync.data.EnumWithValue

class DataRowStateSerializer : EnumByValueSerializer<DataRowState>()

@Serializable(with = DataRowStateSerializer::class)
enum class DataRowState(override val value: Int) : EnumWithValue {
    Detached(1),
    Unchanged(2),
    Added(4),
    Deleted(8),
    Modified(16)
}
