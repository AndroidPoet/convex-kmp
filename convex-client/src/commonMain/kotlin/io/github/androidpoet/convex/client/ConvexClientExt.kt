package io.github.androidpoet.convex.client

import io.github.androidpoet.convex.core.models.PaginationOpts
import io.github.androidpoet.convex.core.models.PaginationResult
import io.github.androidpoet.convex.core.result.ConvexResult
import io.github.androidpoet.convex.core.result.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

public suspend fun ConvexClient.queryPaginated(
    path: String,
    args: JsonObject = JsonObject(emptyMap()),
    paginationOpts: PaginationOpts,
): ConvexResult<PaginationResult> {
    val paginationJson =
        buildJsonObject {
            put("numItems", JsonPrimitive(paginationOpts.numItems))
            if (paginationOpts.cursor != null) {
                put("cursor", JsonPrimitive(paginationOpts.cursor))
            } else {
                put("cursor", kotlinx.serialization.json.JsonNull)
            }
        }

    val mergedArgs =
        JsonObject(
            args.toMutableMap().apply {
                put("paginationOpts", paginationJson)
            },
        )

    return query(path, mergedArgs).map { value ->
        val obj = value.jsonObject
        PaginationResult(
            page = obj["page"]!!.jsonArray.toList(),
            continueCursor = obj["continueCursor"]!!.jsonPrimitive.content,
            isDone = obj["isDone"]!!.jsonPrimitive.content.toBoolean(),
        )
    }
}

public suspend inline fun <reified T> ConvexClient.queryTyped(
    path: String,
    args: JsonObject = JsonObject(emptyMap()),
    json: kotlinx.serialization.json.Json = defaultJson,
): ConvexResult<T> = query(path, args).map { json.decodeFromJsonElement(kotlinx.serialization.serializer<T>(), it) }

public suspend inline fun <reified T> ConvexClient.mutationTyped(
    path: String,
    args: JsonObject = JsonObject(emptyMap()),
    json: kotlinx.serialization.json.Json = defaultJson,
): ConvexResult<T> = mutation(path, args).map { json.decodeFromJsonElement(kotlinx.serialization.serializer<T>(), it) }

public suspend inline fun <reified T> ConvexClient.actionTyped(
    path: String,
    args: JsonObject = JsonObject(emptyMap()),
    json: kotlinx.serialization.json.Json = defaultJson,
): ConvexResult<T> = action(path, args).map { json.decodeFromJsonElement(kotlinx.serialization.serializer<T>(), it) }

@PublishedApi
internal val defaultJson: kotlinx.serialization.json.Json =
    kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
