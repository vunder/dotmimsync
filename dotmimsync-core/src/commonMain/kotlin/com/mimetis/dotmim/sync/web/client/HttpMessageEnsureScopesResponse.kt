package com.mimetis.dotmim.sync.web.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.scopes.ServerScopeInfo

@Serializable
class HttpMessageEnsureScopesResponse(
        @SerialName("sc")
        var syncContext: SyncContext,

        @SerialName("serverscope")
        var serverScopeInfo: ServerScopeInfo
)
