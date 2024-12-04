package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator
import com.mimetis.dotmim.sync.scopes.ServerScopeInfo
import com.mimetis.dotmim.sync.set.SyncSet

class HttpGettingSchemaResponseArgs(
    val serverScopeInfo: ServerScopeInfo,
    val schema: SyncSet,
    context: SyncContext,
    val host: String
) : ProgressArgs(context) {
    override val message: String =
        "Received Schema From Server. Tables Count:${this.schema.tables.size}."

    override val source: String =
        this.host

    override val eventId: Int =
        20250
}

/**
 * Intercept the provider when an http call to get schema is done
 */
fun BaseOrchestrator.onHttpGettingSchemaResponse(action: (HttpGettingSchemaResponseArgs) -> Unit) =
    this.setInterceptor(action)
