package com.mimetis.dotmim.sync.web.client

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.scopes.ScopeInfo
import com.mimetis.dotmim.sync.set.ContainerSet

@Serializable
class HttpMessageSendChangesRequest(
        @SerialName("sc")
        var syncContext: SyncContext,

        @SerialName("scope")
        var scope: ScopeInfo?,

        @SerialName("bi")
        @Required
        var batchIndex: Int = 0,

        @SerialName("bc")
        var batchCount: Int = 0,

        @SerialName("islb")
        @Required
        var isLastBatch: Boolean = false,

        @SerialName("changes")
        @Required
        var changes: ContainerSet? = null
)
