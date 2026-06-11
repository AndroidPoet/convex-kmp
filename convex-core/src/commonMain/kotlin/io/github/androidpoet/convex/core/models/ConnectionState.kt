package io.github.androidpoet.convex.core.models

/**
 * A snapshot of a reactive client's connection to the Convex backend, mirroring
 * the `ConnectionState` object returned by the JavaScript SDK's
 * `ConvexClient.connectionState()`.
 */
public data class ConnectionState(
    /** Whether the WebSocket is currently connected. */
    public val isWebSocketConnected: Boolean = false,
    /** Whether there are mutations or actions awaiting a server response. */
    public val hasInflightRequests: Boolean = false,
    /** Whether a connection to the backend has ever been established. */
    public val hasEverConnected: Boolean = false,
    /** How many times the client has (re)connected. */
    public val connectionCount: Int = 0,
    /** How many times the client has retried after a failed connection. */
    public val connectionRetries: Int = 0,
    /** The number of mutations awaiting a server response. */
    public val inflightMutations: Int = 0,
    /** The number of actions awaiting a server response. */
    public val inflightActions: Int = 0,
)
