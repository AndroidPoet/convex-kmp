package io.github.androidpoet.convex.client.transport

import io.github.androidpoet.convex.client.ConvexConfig
import io.github.androidpoet.convex.client.LogLevel
import io.github.androidpoet.convex.client.auth.AuthState
import io.github.androidpoet.convex.core.models.ConsistentQueryRequest
import io.github.androidpoet.convex.core.models.ConvexResponse
import io.github.androidpoet.convex.core.models.FunctionRequest
import io.github.androidpoet.convex.core.result.ConvexError
import io.github.androidpoet.convex.core.result.ConvexException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal class HttpTransport(
    private val config: ConvexConfig,
    engine: io.ktor.client.engine.HttpClientEngineFactory<*>,
) {
    private var authState: AuthState = AuthState.None

    internal val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }

    internal val httpClient: HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }

            if (config.logging) {
                install(Logging) {
                    logger = Logger.SIMPLE
                    level = config.logLevel.toKtorLevel()
                }
            }

            defaultRequest {
                url(config.deploymentUrl.value.trimEnd('/') + "/")
                contentType(ContentType.Application.Json)
                header("Convex-Client", "kotlin-${SDK_VERSION}")
            }
        }

    private var consistencyTs: Long? = null

    fun setAuth(state: AuthState) {
        authState = state
    }

    fun getAuth(): AuthState = authState

    suspend fun executeFunction(
        endpoint: String,
        path: String,
        args: JsonObject,
    ): ConvexResponse {
        val request = FunctionRequest(path = path, args = args)
        val response =
            httpClient.post(endpoint) {
                authState.headerValue()?.let { header("Authorization", it) }
                setBody(request)
            }
        return parseResponse(response)
    }

    /**
     * Runs a consistent query (experimental). The first call hits `api/query_ts`
     * to obtain a read timestamp; later calls hit `api/query_at_ts` pinned to it,
     * so successive consistent queries observe a mutually consistent snapshot.
     */
    suspend fun executeConsistentQuery(
        path: String,
        args: JsonObject,
    ): ConvexResponse {
        val endpoint = if (consistencyTs == null) "api/query_ts" else "api/query_at_ts"
        val request = ConsistentQueryRequest(path = path, args = args, ts = consistencyTs)
        val response =
            httpClient.post(endpoint) {
                authState.headerValue()?.let { header("Authorization", it) }
                setBody(request)
            }
        val parsed = parseResponse(response)
        parsed.ts?.let { consistencyTs = it }
        return parsed
    }

    private suspend fun parseResponse(response: io.ktor.client.statement.HttpResponse): ConvexResponse =
        when (response.status.value) {
            in 200..299 -> response.body<ConvexResponse>()
            else -> {
                val body = response.bodyAsText()
                throw ConvexException(
                    ConvexError(
                        message = "HTTP ${response.status.value}: $body",
                        data = null,
                    ),
                )
            }
        }

    suspend fun postRaw(
        url: String,
        body: ByteArray,
        contentType: String,
    ): String {
        val response =
            httpClient.post(url) {
                authState.headerValue()?.let { header("Authorization", it) }
                contentType(ContentType.parse(contentType))
                setBody(body)
            }
        return response.bodyAsText()
    }

    fun close() {
        httpClient.close()
    }

    companion object {
        const val SDK_VERSION = "0.1.0"
    }
}

private fun LogLevel.toKtorLevel(): io.ktor.client.plugins.logging.LogLevel =
    when (this) {
        LogLevel.ALL -> io.ktor.client.plugins.logging.LogLevel.ALL
        LogLevel.HEADERS -> io.ktor.client.plugins.logging.LogLevel.HEADERS
        LogLevel.BODY -> io.ktor.client.plugins.logging.LogLevel.BODY
        LogLevel.INFO -> io.ktor.client.plugins.logging.LogLevel.INFO
        LogLevel.NONE -> io.ktor.client.plugins.logging.LogLevel.NONE
    }
