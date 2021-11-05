package com.mimetis.dotmim.sync.web.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.SyncContext

@Serializable
class HttpMessageGetMoreChangesRequest(
        @SerialName("sc")
        var syncContext: SyncContext,

        @SerialName("bireq")
        var batchIndexRequested: Int
)
