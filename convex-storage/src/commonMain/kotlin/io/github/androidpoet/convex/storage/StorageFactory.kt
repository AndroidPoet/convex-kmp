package io.github.androidpoet.convex.storage

import io.github.androidpoet.convex.client.ConvexClient
import kotlinx.serialization.json.Json

public fun createStorageClient(client: ConvexClient): StorageClient =
    StorageClientImpl(
        client = client,
        json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            },
    )
