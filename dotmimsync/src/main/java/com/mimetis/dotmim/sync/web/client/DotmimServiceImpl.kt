package com.mimetis.dotmim.sync.web.client

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import com.mimetis.dotmim.sync.serialization.Converter
import com.mimetis.dotmim.sync.web.client.*
import okhttp3.JavaNetCookieJar
import ru.gildor.coroutines.okhttp.await
import java.net.CookieManager
import java.util.*
import java.util.concurrent.TimeUnit

class DotmimServiceImpl {
    private interface DotmimServiceInternal {
        @POST("sync")
        suspend fun ensureSchema(
            @Header("AuthHeader")
            authHeader: String,

            @Header("dotmim-sync-session-id")
            sessionId: String,

            @Header("dotmim-sync-scope-name")
            scopeName: String,

            @Header("dotmim-sync-step")
            step: Int,

            @Body
            args: HttpMessageEnsureScopesRequest,

            @Header("dotmim-sync-converter")
            converterKey: String? = null
        ): HttpMessageEnsureSchemaResponse

        @POST("sync")
        suspend fun ensureScope(
            @Header("AuthHeader")
            authHeader: String,

            @Header("dotmim-sync-session-id")
            sessionId: String,

            @Header("dotmim-sync-scope-name")
            scopeName: String,

            @Header("dotmim-sync-step")
            step: Int,

            @Body
            args: HttpMessageEnsureScopesRequest,

            @Header("dotmim-sync-converter")
            converterKey: String? = null
        ): HttpMessageEnsureScopesResponse

        /**
         * Used for both SendChanges and GetChanges
         * @see HttpStep.SendChanges
         * @see HttpStep.GetChanges
         * @see HttpStep.GetSnapshot
         * @see HttpStep.GetEstimatedChangesCount
         */
        @POST("sync")
        suspend fun changes(
            @Header("AuthHeader")
            authHeader: String,

            @Header("dotmim-sync-session-id")
            sessionId: String,

            @Header("dotmim-sync-scope-name")
            scopeName: String,

            @Header("dotmim-sync-step")
            step: Int,

            @Body
            args: HttpMessageSendChangesRequest,

            @Header("dotmim-sync-converter")
            converterKey: String? = null
        ): HttpMessageSummaryResponse

//        @POST("sync")
//        suspend fun sendMoreChanges(
//                @Header("AuthHeader")
//                authHeader: String,
//
//                @Header("dotmim-sync-session-id")
//                sessionId: String,
//
//                @Header("dotmim-sync-scope-name")
//                scopeName: String,
//
//                @Header("dotmim-sync-step")
//                step: Int,
//
//                @Body
//                args: HttpMessageGetMoreChangesRequest,
//
//                @Header("dotmim-sync-converter")
//                converterKey: String? = null
//        ): HttpMessageSendChangesResponse

        @POST("sync")
        suspend fun endOfDownloadChanges(
            @Header("AuthHeader")
            authHeader: String,

            @Header("dotmim-sync-session-id")
            sessionId: String,

            @Header("dotmim-sync-scope-name")
            scopeName: String,

            @Header("dotmim-sync-step")
            step: Int,

            @Body
            args: HttpMessageGetMoreChangesRequest,

            @Header("dotmim-sync-converter")
            converterKey: String? = null
        )

        @POST("sync")
        suspend fun summary(
            @Header("AuthHeader")
            authHeader: String,

            @Header("dotmim-sync-session-id")
            sessionId: String,

            @Header("dotmim-sync-scope-name")
            scopeName: String,

            @Header("dotmim-sync-step")
            step: Int,

            @Body
            args: HttpMessageSendChangesRequest,

            @Header("dotmim-sync-converter")
            converterKey: String? = null
        ): HttpMessageSummaryResponse
    }

    private val okHttpClient: OkHttpClient
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val serviceAddress: String = "https://money-wallet.net/sync-api/"
//    val serviceAddress: String = "http://192.168.0.206:6000/"

    private var service: DotmimServiceInternal

    suspend fun ensureScope(
        authHeader: String,
        args: HttpMessageEnsureScopesRequest,
        converter: Converter?
    ): HttpMessageEnsureScopesResponse =
        service.ensureScope(
            authHeader,
            args.syncContext.sessionId.toString(),
            args.syncContext.scopeName,
            HttpStep.EnsureScopes.value,
            args,
            converter?.key
        )

    suspend fun ensureSchema(
        authHeader: String,
        args: HttpMessageEnsureScopesRequest,
        converter: Converter?
    ): HttpMessageEnsureSchemaResponse =
        service.ensureSchema(
            authHeader,
            args.syncContext.sessionId.toString(),
            args.syncContext.scopeName,
            HttpStep.EnsureSchema.value,
            args,
            converter?.key
        )

    suspend fun getSnapshot(
        authHeader: String,
        args: HttpMessageSendChangesRequest,
        converter: Converter?
    ): HttpMessageSummaryResponse =
        service.changes(
            authHeader,
            args.syncContext.sessionId.toString(),
            args.syncContext.scopeName,
            HttpStep.GetSnapshot.value,
            args,
            converter?.key
        )

    suspend fun sendChanges(
        authHeader: String,
        args: HttpMessageSendChangesRequest,
        converter: Converter?
    ): HttpMessageSummaryResponse =
        service.changes(
            authHeader,
            args.syncContext.sessionId.toString(),
            args.syncContext.scopeName,
            HttpStep.SendChangesInProgress.value,
            args,
            converter?.key
        )

    suspend fun moreChanges(
        authHeader: String,
        args: HttpMessageGetMoreChangesRequest,
        converter: Converter?
    ): ByteArray {
        val requestBuilder = Request.Builder()
            .url("${serviceAddress}sync")
            .header("AuthHeader", authHeader)
            .header("dotmim-sync-session-id", args.syncContext.sessionId.toString())
            .header("dotmim-sync-scope-name", args.syncContext.scopeName)
            .header("dotmim-sync-step", HttpStep.GetSummary.value.toString())
            .post(json.encodeToString(args).toRequestBody("application/json".toMediaType()))
        if (converter != null)
            requestBuilder.header("dotmim-sync-converter", converter.key)
        val response = okHttpClient.newCall(requestBuilder.build()).await()
        return response.body!!.bytes()
//        return service.sendMoreChanges(authHeader, args.syncContext.sessionId.toString(), args.syncContext.scopeName, HttpStep.GetMoreChanges.value, args, converter?.key)
    }

    suspend fun endDownloadChanges(
        authHeader: String,
        args: HttpMessageGetMoreChangesRequest,
        converter: Converter?
    ) {
        service.endOfDownloadChanges(
            authHeader,
            args.syncContext.sessionId.toString(),
            args.syncContext.scopeName,
            HttpStep.SendEndDownloadChanges.value,
            args,
            converter?.key
        )
    }

    suspend fun getSummary(
        authHeader: String,
        args: HttpMessageSendChangesRequest,
        converter: Converter?
    ): HttpMessageSummaryResponse =
        service.summary(
            authHeader,
            args.syncContext.sessionId.toString(),
            args.syncContext.scopeName,
            HttpStep.GetSummary.value,
            args,
            converter?.key
        )

    init {
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .cookieJar(JavaNetCookieJar(CookieManager()))
            .addInterceptor { chain: Interceptor.Chain ->
                val buffer = Buffer()
                chain.request().body?.writeTo(buffer)
                val bodyHash = buffer.sha256().base64()
                val authHeader = chain.request().header("AuthHeader") ?: ""
                val request = chain.request().newBuilder()
                    .removeHeader("AuthHeader")
                    .addHeader("Authorization", authHeader)
                    .addHeader("dotmim-sync-serialization-format", "{\"f\":\"json\", \"s\":0}")
                    .addHeader("dotmim-sync-hash", bodyHash)
                    .build()

                val response = chain.proceed(request)
//                if (!response.isSuccessful)
//                    CrashesHelper.logError(response, "DotmimServiceImpl.request")

                return@addInterceptor response
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(serviceAddress)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = retrofit.create(DotmimServiceInternal::class.java)
    }

//    companion object {
//        private val TAG = DotmimServiceImpl::class.java.simpleName
//    }
}
