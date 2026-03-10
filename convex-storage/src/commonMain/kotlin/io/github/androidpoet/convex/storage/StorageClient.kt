package io.github.androidpoet.convex.storage

import io.github.androidpoet.convex.client.ConvexClient
import io.github.androidpoet.convex.core.models.UploadResponse
import io.github.androidpoet.convex.core.result.ConvexResult
import io.github.androidpoet.convex.core.result.map
import io.github.androidpoet.convex.core.types.StorageId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

public interface StorageClient {

    public suspend fun generateUploadUrl(
        functionPath: String = "storage:generateUploadUrl",
        args: JsonObject = JsonObject(emptyMap()),
    ): ConvexResult<String>

    public suspend fun upload(
        uploadUrl: String,
        data: ByteArray,
        contentType: String = "application/octet-stream",
    ): ConvexResult<StorageId>

    public suspend fun getUrl(
        functionPath: String = "storage:getUrl",
        storageId: StorageId,
    ): ConvexResult<String?>

    public suspend fun deleteFile(
        functionPath: String = "storage:deleteFile",
        storageId: StorageId,
    ): ConvexResult<Unit>
}

internal class StorageClientImpl(
    private val client: ConvexClient,
    private val json: Json,
) : StorageClient {

    override suspend fun generateUploadUrl(
        functionPath: String,
        args: JsonObject,
    ): ConvexResult<String> =
        client.mutation(functionPath, args).map { value ->
            value.jsonPrimitive.content
        }

    override suspend fun upload(
        uploadUrl: String,
        data: ByteArray,
        contentType: String,
    ): ConvexResult<StorageId> = ConvexResult.catching {
        val responseBody = client.postRaw(uploadUrl, data, contentType)
        val response = json.decodeFromString<UploadResponse>(responseBody)
        StorageId(response.storageId)
    }

    override suspend fun getUrl(
        functionPath: String,
        storageId: StorageId,
    ): ConvexResult<String?> {
        val args = JsonObject(
            mapOf("storageId" to JsonPrimitive(storageId.value)),
        )
        return client.query(functionPath, args).map { value ->
            if (value is JsonNull) null
            else value.jsonPrimitive.content
        }
    }

    override suspend fun deleteFile(
        functionPath: String,
        storageId: StorageId,
    ): ConvexResult<Unit> {
        val args = JsonObject(
            mapOf("storageId" to JsonPrimitive(storageId.value)),
        )
        return client.mutation(functionPath, args).map { }
    }
}
