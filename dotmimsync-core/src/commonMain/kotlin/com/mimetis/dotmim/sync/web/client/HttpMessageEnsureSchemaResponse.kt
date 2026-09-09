package com.mimetis.dotmim.sync.web.client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.scopes.ServerScopeInfo
import com.mimetis.dotmim.sync.set.SyncSet

@Serializable
class HttpMessageEnsureSchemaResponse(
        @SerialName("sc")
        var syncContext: SyncContext,

        /**
         * Gets or Sets the schema because the ServerScopeInfo won't have it since it's marked (on purpose) as IgnoreDataMember (and then not serialized)
         */
        @SerialName("schema")
        var schema: SyncSet,

        /**
         * Gets or Sets the server scope info, from server
         */
        @SerialName("ssi")
        var serverScopeInfo: ServerScopeInfo
)
