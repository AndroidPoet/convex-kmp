package io.github.androidpoet.convex.realtime

import io.github.androidpoet.convex.client.ConvexClient
import io.github.androidpoet.convex.core.result.ConvexResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

public interface RealtimeClient {
    public fun subscribe(
        path: String,
        args: JsonObject = JsonObject(emptyMap()),
        intervalMs: Long = 1000L,
    ): Flow<ConvexResult<JsonElement>>

    public fun close()
}

internal class PollingRealtimeClient(
    private val client: ConvexClient,
) : RealtimeClient {
    override fun subscribe(
        path: String,
        args: JsonObject,
        intervalMs: Long,
    ): Flow<ConvexResult<JsonElement>> =
        flow {
            while (currentCoroutineContext().isActive) {
                emit(client.query(path, args))
                delay(intervalMs)
            }
        }

    override fun close() {
        // No persistent connection to close in polling mode
    }
}
