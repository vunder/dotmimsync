package com.mimetis.dotmim.sync.web.client

import com.mimetis.dotmim.sync.serialization.Converter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
internal class DotmimServiceImpl(
    serviceAddress: String,
    val client: HttpClient
) {
    private val url = serviceAddress.trimEnd('/') + "/sync"

    suspend fun ensureScope(
        authHeader: String,
        args: HttpMessageEnsureScopesRequest,
        converter: Converter?
    ): HttpMessageEnsureScopesResponse =
        client.post(url) {
            headers.append(HttpHeaders.Authorization, authHeader)
            headers.append("dotmim-sync-session-id", args.syncContext.sessionId.toString())
            headers.append("dotmim-sync-scope-name", args.syncContext.scopeName)
            headers.append("dotmim-sync-step", HttpStep.EnsureScopes.value.toString())
            if (converter != null)
                headers.append("dotmim-sync-converter", converter.key)

            contentType(ContentType.Application.Json)
            setBody(args)
        }.body()

    suspend fun ensureSchema(
        authHeader: String,
        args: HttpMessageEnsureScopesRequest,
        converter: Converter?
    ): HttpMessageEnsureSchemaResponse =
        client.post(url) {
            headers.append(HttpHeaders.Authorization, authHeader)
            headers.append("dotmim-sync-session-id", args.syncContext.sessionId.toString())
            headers.append("dotmim-sync-scope-name", args.syncContext.scopeName)
            headers.append("dotmim-sync-step", HttpStep.EnsureSchema.value.toString())
            if (converter != null)
                headers.append("dotmim-sync-converter", converter.key)

            contentType(ContentType.Application.Json)
            setBody(args)
        }.body()

    suspend fun getSnapshot(
        authHeader: String,
        args: HttpMessageSendChangesRequest,
        converter: Converter?
    ): HttpMessageSummaryResponse =
        client.post(url) {
            headers.append(HttpHeaders.Authorization, authHeader)
            headers.append("dotmim-sync-session-id", args.syncContext.sessionId.toString())
            headers.append("dotmim-sync-scope-name", args.syncContext.scopeName)
            headers.append("dotmim-sync-step", HttpStep.GetSnapshot.value.toString())
            if (converter != null)
                headers.append("dotmim-sync-converter", converter.key)

            contentType(ContentType.Application.Json)
            setBody(args)
        }.body()

    suspend fun sendChanges(
        authHeader: String,
        args: HttpMessageSendChangesRequest,
        converter: Converter?
    ): HttpMessageSummaryResponse =
        client.post(url) {
            headers.append(HttpHeaders.Authorization, authHeader)
            headers.append("dotmim-sync-session-id", args.syncContext.sessionId.toString())
            headers.append("dotmim-sync-scope-name", args.syncContext.scopeName)
            headers.append("dotmim-sync-step", HttpStep.SendChangesInProgress.value.toString())
            if (converter != null)
                headers.append("dotmim-sync-converter", converter.key)

            contentType(ContentType.Application.Json)
            setBody(args)
        }.body()

    suspend fun moreChanges(
        authHeader: String,
        args: HttpMessageGetMoreChangesRequest,
        converter: Converter?
    ): ByteArray =
        client.post(url) {
            headers.append(HttpHeaders.Authorization, authHeader)
            headers.append("dotmim-sync-session-id", args.syncContext.sessionId.toString())
            headers.append("dotmim-sync-scope-name", args.syncContext.scopeName)
            headers.append("dotmim-sync-step", HttpStep.GetSummary.value.toString())
            if (converter != null)
                headers.append("dotmim-sync-converter", converter.key)

            contentType(ContentType.Application.Json)
            setBody(args)
        }.readBytes()

    suspend fun endDownloadChanges(
        authHeader: String,
        args: HttpMessageGetMoreChangesRequest,
        converter: Converter?
    ): HttpMessageGetMoreChangesRequest =
        client.post(url) {
            headers.append(HttpHeaders.Authorization, authHeader)
            headers.append("dotmim-sync-session-id", args.syncContext.sessionId.toString())
            headers.append("dotmim-sync-scope-name", args.syncContext.scopeName)
            headers.append("dotmim-sync-step", HttpStep.SendEndDownloadChanges.value.toString())
            if (converter != null)
                headers.append("dotmim-sync-converter", converter.key)

            contentType(ContentType.Application.Json)
            setBody(args)
        }.body()

    suspend fun getSummary(
        authHeader: String,
        args: HttpMessageSendChangesRequest,
        converter: Converter?
    ): HttpMessageSummaryResponse =
        client.post(url) {
            headers.append(HttpHeaders.Authorization, authHeader)
            headers.append("dotmim-sync-session-id", args.syncContext.sessionId.toString())
            headers.append("dotmim-sync-scope-name", args.syncContext.scopeName)
            headers.append("dotmim-sync-step", HttpStep.GetSummary.value.toString())
            if (converter != null)
                headers.append("dotmim-sync-converter", converter.key)

            contentType(ContentType.Application.Json)
            setBody(args)
        }.body()

    init {
        val md = MessageDigest.getInstance("SHA-256")
        client.plugin(HttpSend).intercept { request ->
            request.headers.append("dotmim-sync-serialization-format", """{"f":"json", "s":0}""")
            if (request.body is OutgoingContent.ByteArrayContent) {
                val bytes = (request.body as OutgoingContent.ByteArrayContent).bytes()
                val digest = md.digest(bytes)
                val hash = Base64.encode(digest)
                request.headers.append("dotmim-sync-hash", hash)
            }
            execute(request)
        }
    }
}
