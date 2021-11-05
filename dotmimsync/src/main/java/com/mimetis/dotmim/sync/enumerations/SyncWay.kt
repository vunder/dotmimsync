package com.mimetis.dotmim.sync.enumerations

import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.data.EnumByValueSerializer
import com.mimetis.dotmim.sync.data.EnumWithValue

class SyncWaySerializer : EnumByValueSerializer<SyncWay>()

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
