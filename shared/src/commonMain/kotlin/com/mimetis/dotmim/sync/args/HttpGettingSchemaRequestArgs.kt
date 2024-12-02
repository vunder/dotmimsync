package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Represents a request made to the server to get the server scope info
 */
class HttpGettingSchemaRequestArgs(
    context: SyncContext,
    val host: String
) : ProgressArgs(context) {
    override val message: String =
        "Getting Server Schema. Scope Name:${this.context.scopeName}."

    override val eventId: Int =
        20200

    override val source: String =
        this.host
}

/**
 * Intercept the provider when an http call is about to be made to get server schema
 */
fun BaseOrchestrator.onHttpGettingSchemaRequest(action: (HttpGettingSchemaRequestArgs) -> Unit) =
    this.setInterceptor(action)
