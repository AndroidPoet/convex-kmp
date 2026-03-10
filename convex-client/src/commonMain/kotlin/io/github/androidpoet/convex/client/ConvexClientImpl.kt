package io.github.androidpoet.convex.client

import io.github.androidpoet.convex.client.auth.AuthState
import io.github.androidpoet.convex.client.transport.HttpTransport
import io.github.androidpoet.convex.core.result.ConvexError
import io.github.androidpoet.convex.core.result.ConvexResult
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

internal class ConvexClientImpl(
    private val transport: HttpTransport,
) : ConvexClient {

    override suspend fun query(
        path: String,
        args: JsonObject,
    ): ConvexResult<JsonElement> = executeFunction("api/query", path, args)

    override suspend fun mutation(
        path: String,
        args: JsonObject,
    ): ConvexResult<JsonElement> = executeFunction("api/mutation", path, args)

    override suspend fun action(
        path: String,
        args: JsonObject,
    ): ConvexResult<JsonElement> = executeFunction("api/action", path, args)

    override suspend fun postRaw(
        url: String,
        body: ByteArray,
        contentType: String,
    ): String = transport.postRaw(url, body, contentType)

    override fun setAuth(token: String) {
        transport.setAuth(AuthState.Bearer(token))
    }

    override fun setAdminAuth(deployKey: String) {
        transport.setAuth(AuthState.Admin(deployKey))
    }

    override fun clearAuth() {
        transport.setAuth(AuthState.None)
    }

    override fun close() {
        transport.close()
    }

    private suspend fun executeFunction(
        endpoint: String,
        path: String,
        args: JsonObject,
    ): ConvexResult<JsonElement> = ConvexResult.catching {
        val response = transport.executeFunction(endpoint, path, args)
        if (response.isSuccess) {
            response.value ?: JsonNull
        } else {
            throw ConvexError(
                message = response.errorMessage ?: "Unknown error",
                data = response.errorData,
            ).toException()
        }
    }
}
