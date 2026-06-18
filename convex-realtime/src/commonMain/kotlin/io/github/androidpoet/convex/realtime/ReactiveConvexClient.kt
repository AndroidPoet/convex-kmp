package io.github.androidpoet.convex.realtime

import io.github.androidpoet.convex.core.models.ConnectionState
import io.github.androidpoet.convex.core.result.ConvexResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * A reactive Convex client backed by the sync WebSocket protocol, mirroring the
 * JavaScript SDK's `ConvexClient`.
 *
 * Unlike the polling [RealtimeClient], [onUpdate] opens a real query subscription:
 * the backend pushes a new value every time the query result changes, with no
 * re-polling. Collecting the returned [Flow] subscribes; cancelling the collector
 * unsubscribes. Multiple collectors of the same `(path, args)` share a single
 * server-side subscription.
 */
public interface ReactiveConvexClient {
    /** The live connection state, mirroring `ConvexClient.connectionState()`. */
    public val connectionState: StateFlow<ConnectionState>

    /**
     * Subscribes to a query and emits its result on every server-pushed change,
     * mirroring `ConvexClient.onUpdate`. The first emission is the initial result.
     */
    public fun onUpdate(
        path: String,
        args: JsonObject = JsonObject(emptyMap()),
    ): Flow<ConvexResult<JsonElement>>

    /**
     * Runs a query once over the WebSocket, returning the first result and then
     * unsubscribing. For continuous updates use [onUpdate].
     */
    public suspend fun query(
        path: String,
        args: JsonObject = JsonObject(emptyMap()),
    ): ConvexResult<JsonElement>

    /** Runs a mutation over the WebSocket, mirroring `ConvexClient.mutation`. */
    public suspend fun mutation(
        path: String,
        args: JsonObject = JsonObject(emptyMap()),
    ): ConvexResult<JsonElement>

    /** Runs an action over the WebSocket, mirroring `ConvexClient.action`. */
    public suspend fun action(
        path: String,
        args: JsonObject = JsonObject(emptyMap()),
    ): ConvexResult<JsonElement>

    /**
     * Sets (or, with `null`, clears) the auth token sent to the backend,
     * mirroring `ConvexClient.setAuth` / `clearAuth`.
     */
    public fun setAuth(token: String?)

    /** Closes the WebSocket and releases all resources. */
    public fun close()
}

@DslMarker
public annotation class ReactiveConfigDsl

@ReactiveConfigDsl
public class ReactiveConfigBuilder internal constructor() {
    /** The sync endpoint path appended to the deployment URL. */
    public var syncPath: String = "/api/sync"

    /** Initial reconnect backoff after a dropped connection. */
    public var reconnectInitialDelayMs: Long = 500L

    /** Maximum reconnect backoff. */
    public var reconnectMaxDelayMs: Long = 30_000L
}

/**
 * Creates a [ReactiveConvexClient] connected to [deploymentUrl] (e.g.
 * `https://happy-animal-123.convex.cloud`). The WebSocket connects lazily on the
 * first subscription, mutation, or action.
 */
public fun createReactiveClient(
    deploymentUrl: String,
    configure: ReactiveConfigBuilder.() -> Unit = {},
): ReactiveConvexClient {
    val config = ReactiveConfigBuilder().apply(configure)
    return WebSocketConvexClient(
        deploymentUrl = deploymentUrl,
        syncPath = config.syncPath,
        reconnectInitialDelayMs = config.reconnectInitialDelayMs,
        reconnectMaxDelayMs = config.reconnectMaxDelayMs,
    )
}
