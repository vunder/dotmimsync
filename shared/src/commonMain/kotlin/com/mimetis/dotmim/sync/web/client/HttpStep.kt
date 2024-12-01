package com.mimetis.dotmim.sync.web.client

import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.data.EnumByValueSerializer
import com.mimetis.dotmim.sync.data.EnumWithValue

class HttpStepSerializer : EnumByValueSerializer<HttpStep>()

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
