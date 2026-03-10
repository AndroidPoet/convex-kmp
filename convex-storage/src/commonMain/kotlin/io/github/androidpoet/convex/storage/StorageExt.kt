package io.github.androidpoet.convex.storage

import io.github.androidpoet.convex.core.result.ConvexResult
import io.github.androidpoet.convex.core.result.flatMap
import io.github.androidpoet.convex.core.types.StorageId

public suspend fun StorageClient.uploadFile(
    data: ByteArray,
    contentType: String = "application/octet-stream",
    generateUrlPath: String = "storage:generateUploadUrl",
): ConvexResult<StorageId> =
    generateUploadUrl(generateUrlPath).flatMap { url ->
        upload(url, data, contentType)
    }
