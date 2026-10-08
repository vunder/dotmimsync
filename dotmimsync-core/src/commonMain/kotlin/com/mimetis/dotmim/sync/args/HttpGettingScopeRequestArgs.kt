package com.mimetis.dotmim.sync.args

import com.mimetis.dotmim.sync.SyncContext
import com.mimetis.dotmim.sync.orchestrators.BaseOrchestrator

/**
 * Represents a request made to the server to get the server scope info
 */
class HttpGettingScopeRequestArgs(
    context: SyncContext,
    val host: String
) : ProgressArgs(context) {
    override val message: String =
        "Getting Server Scope. Scope Name:${this.context.scopeName}."
    override val eventId: Int = 20300
    override val source: String = host
}

/**
 * Intercept the provider when an http is about to be done to get server scope
 */
fun BaseOrchestrator.onHttpGettingScopeRequest(action: (HttpGettingScopeRequestArgs) -> Unit) =
    this.setInterceptor(action)
