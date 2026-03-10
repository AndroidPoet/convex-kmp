package io.github.androidpoet.convex.client

import io.github.androidpoet.convex.core.result.ConvexResult
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

public interface ConvexClient {

    public suspend fun query(
        path: String,
        args: JsonObject = JsonObject(emptyMap()),
    ): ConvexResult<JsonElement>

    public suspend fun mutation(
        path: String,
        args: JsonObject = JsonObject(emptyMap()),
    ): ConvexResult<JsonElement>

    public suspend fun action(
        path: String,
        args: JsonObject = JsonObject(emptyMap()),
    ): ConvexResult<JsonElement>

    public suspend fun postRaw(
        url: String,
        body: ByteArray,
        contentType: String,
    ): String

    public fun setAuth(token: String)

    public fun setAdminAuth(deployKey: String)

    public fun clearAuth()

    public fun close()
}
