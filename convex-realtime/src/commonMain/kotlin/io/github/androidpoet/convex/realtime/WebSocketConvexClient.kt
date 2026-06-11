package io.github.androidpoet.convex.realtime

import io.github.androidpoet.convex.core.models.ConnectionState
import io.github.androidpoet.convex.core.result.ConvexError
import io.github.androidpoet.convex.core.result.ConvexResult
import io.github.androidpoet.convex.core.values.ConvexCodec
import io.github.androidpoet.convex.realtime.protocol.ActionMessage
import io.github.androidpoet.convex.realtime.protocol.ActionResponse
import io.github.androidpoet.convex.realtime.protocol.AddQuery
import io.github.androidpoet.convex.realtime.protocol.AuthError
import io.github.androidpoet.convex.realtime.protocol.Authenticate
import io.github.androidpoet.convex.realtime.protocol.ClientMessage
import io.github.androidpoet.convex.realtime.protocol.Connect
import io.github.androidpoet.convex.realtime.protocol.FatalError
import io.github.androidpoet.convex.realtime.protocol.ModifyQuerySet
import io.github.androidpoet.convex.realtime.protocol.MutationMessage
import io.github.androidpoet.convex.realtime.protocol.MutationResponse
import io.github.androidpoet.convex.realtime.protocol.Ping
import io.github.androidpoet.convex.realtime.protocol.QueryFailed
import io.github.androidpoet.convex.realtime.protocol.QueryRemoved
import io.github.androidpoet.convex.realtime.protocol.QueryUpdated
import io.github.androidpoet.convex.realtime.protocol.RemoveQuery
import io.github.androidpoet.convex.realtime.protocol.ServerMessage
import io.github.androidpoet.convex.realtime.protocol.Transition
import io.github.androidpoet.convex.realtime.transport.platformEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The Convex sync-protocol client over a WebSocket. Maintains the active query
 * set, applies server [Transition]s to per-query [MutableStateFlow]s, correlates
 * mutation/action responses by request id, and transparently reconnects with
 * exponential backoff (re-sending the query set, auth, and any in-flight calls).
 */
@OptIn(ExperimentalUuidApi::class)
internal class WebSocketConvexClient(
    deploymentUrl: String,
    syncPath: String,
    private val reconnectInitialDelayMs: Long,
    private val reconnectMaxDelayMs: Long,
) : ReactiveConvexClient {

    private val wsUrl = buildWsUrl(deploymentUrl, syncPath)
    private val sessionId = Uuid.random().toString()

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val httpClient = HttpClient(platformEngine()) {
        install(WebSockets)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sendMutex = Mutex()
    private val stateMutex = Mutex()

    private var session: DefaultClientWebSocketSession? = null

    // Query subscriptions, keyed both by stable token and by server-assigned id.
    private val queriesByToken = mutableMapOf<String, QueryState>()
    private val queriesById = mutableMapOf<Int, QueryState>()
    private var queryIdCounter = 0
    private var querySetVersion = 0

    // In-flight mutations/actions awaiting a response (resent on reconnect).
    private val pendingRequests = mutableMapOf<Int, CompletableDeferred<ConvexResult<JsonElement>>>()
    private val pendingMessages = mutableMapOf<Int, ClientMessage>()
    private var requestIdCounter = 0
    private var inflightMutations = 0
    private var inflightActions = 0

    private var authToken: String? = null
    private var connectionCount = 0
    private var maxObservedTimestamp: Long? = null

    private val _connectionState = MutableStateFlow(ConnectionState())
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    init {
        scope.launch { connectionLoop() }
    }

    private class QueryState(
        val queryId: Int,
        val udfPath: String,
        val args: JsonObject,
        val flow: MutableStateFlow<ConvexResult<JsonElement>?>,
        var refCount: Int,
    )

    // ── Public API ──────────────────────────────────────────────────────

    override fun onUpdate(
        path: String,
        args: JsonObject,
    ): Flow<ConvexResult<JsonElement>> = flow {
        val token = queryToken(path, args)
        val (state, isNew) = stateMutex.withLock {
            val existing = queriesByToken[token]
            if (existing != null) {
                existing.refCount++
                existing to false
            } else {
                val id = queryIdCounter++
                val created = QueryState(id, path, args, MutableStateFlow(null), 1)
                queriesByToken[token] = created
                queriesById[id] = created
                created to true
            }
        }
        if (isNew) sendQuerySetModification(listOf(AddQuery(state.queryId, path, listOf(args))))
        try {
            state.flow.filterNotNull().collect { emit(it) }
        } finally {
            unsubscribe(token, state)
        }
    }

    override suspend fun query(path: String, args: JsonObject): ConvexResult<JsonElement> =
        onUpdate(path, args).first()

    override suspend fun mutation(path: String, args: JsonObject): ConvexResult<JsonElement> =
        request(isMutation = true, path = path, args = args)

    override suspend fun action(path: String, args: JsonObject): ConvexResult<JsonElement> =
        request(isMutation = false, path = path, args = args)

    override fun setAuth(token: String?) {
        scope.launch {
            stateMutex.withLock { authToken = token }
            val current = session ?: return@launch
            if (token != null) {
                sendRaw(current, Authenticate(baseVersion = 0, tokenType = "User", value = token))
            }
        }
    }

    override fun close() {
        scope.launch {
            stateMutex.withLock {
                pendingRequests.values.forEach {
                    it.complete(
                        ConvexResult.Failure(ConvexError("Client closed", null)),
                    )
                }
                pendingRequests.clear()
                pendingMessages.clear()
            }
        }
        scope.cancel()
        httpClient.close()
    }

    // ── Request/response correlation ────────────────────────────────────

    private suspend fun request(
        isMutation: Boolean,
        path: String,
        args: JsonObject,
    ): ConvexResult<JsonElement> {
        val deferred = CompletableDeferred<ConvexResult<JsonElement>>()
        val (requestId, message) = stateMutex.withLock {
            val id = requestIdCounter++
            val msg: ClientMessage = if (isMutation) {
                MutationMessage(id, path, listOf(args))
            } else {
                ActionMessage(id, path, listOf(args))
            }
            pendingRequests[id] = deferred
            pendingMessages[id] = msg
            if (isMutation) inflightMutations++ else inflightActions++
            id to msg
        }
        refreshInflightState()
        session?.let { sendRaw(it, message) }
        return try {
            deferred.await()
        } finally {
            stateMutex.withLock {
                if (pendingRequests.remove(requestId) != null) {
                    pendingMessages.remove(requestId)
                    if (isMutation) inflightMutations-- else inflightActions--
                }
            }
            refreshInflightState()
        }
    }

    private suspend fun completeRequest(
        requestId: Int,
        success: Boolean,
        result: JsonElement?,
        errorMessage: String?,
        errorData: JsonElement?,
    ) {
        val deferred = stateMutex.withLock {
            pendingMessages.remove(requestId)
            pendingRequests.remove(requestId)
        } ?: return
        val outcome = if (success) {
            ConvexResult.Success(ConvexCodec.decodeToPlainJson(result ?: JsonNull))
        } else {
            ConvexResult.Failure(
                ConvexError(
                    message = errorMessage ?: "Unknown error",
                    data = errorData?.let { ConvexCodec.decodeToPlainJson(it) },
                ),
            )
        }
        deferred.complete(outcome)
    }

    // ── Subscription bookkeeping ────────────────────────────────────────

    private suspend fun unsubscribe(token: String, state: QueryState) {
        val removed = stateMutex.withLock {
            state.refCount--
            if (state.refCount <= 0) {
                queriesByToken.remove(token)
                queriesById.remove(state.queryId)
                true
            } else {
                false
            }
        }
        if (removed) sendQuerySetModification(listOf(RemoveQuery(state.queryId)))
    }

    private suspend fun sendQuerySetModification(
        modifications: List<io.github.androidpoet.convex.realtime.protocol.QuerySetModification>,
    ) {
        val current = session ?: return // resynced in full on reconnect
        val message = stateMutex.withLock {
            val base = querySetVersion
            querySetVersion += modifications.size
            ModifyQuerySet(baseVersion = base, newVersion = querySetVersion, modifications = modifications)
        }
        sendRaw(current, message)
    }

    // ── Connection lifecycle ────────────────────────────────────────────

    private suspend fun connectionLoop() {
        var retries = 0
        while (scope.isActive) {
            try {
                httpClient.webSocket(urlString = wsUrl) {
                    session = this
                    retries = 0
                    onConnected()
                    for (frame in incoming) {
                        if (frame is Frame.Text) handleMessage(frame.readText())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Connection dropped; fall through to backoff + retry.
            }
            session = null
            _connectionState.update { it.copy(isWebSocketConnected = false) }
            if (!scope.isActive) break
            retries++
            _connectionState.update { it.copy(connectionRetries = retries) }
            val backoff = reconnectInitialDelayMs shl minOf(retries - 1, 6)
            kotlinx.coroutines.delay(minOf(reconnectMaxDelayMs, backoff))
        }
    }

    private suspend fun DefaultClientWebSocketSession.onConnected() {
        val (count, token, adds, pending, ts) = stateMutex.withLock {
            connectionCount++
            querySetVersion = 0
            val addList = queriesById.values.map { AddQuery(it.queryId, it.udfPath, listOf(it.args)) }
            querySetVersion = addList.size
            ConnectSnapshot(connectionCount, authToken, addList, pendingMessages.values.toList(), maxObservedTimestamp)
        }
        sendRaw(this, Connect(sessionId, count, maxObservedTimestamp = ts))
        token?.let { sendRaw(this, Authenticate(baseVersion = 0, tokenType = "User", value = it)) }
        if (adds.isNotEmpty()) {
            sendRaw(this, ModifyQuerySet(baseVersion = 0, newVersion = adds.size, modifications = adds))
        }
        pending.forEach { sendRaw(this, it) }
        _connectionState.update {
            it.copy(
                isWebSocketConnected = true,
                hasEverConnected = true,
                connectionCount = count,
                connectionRetries = 0,
            )
        }
    }

    private data class ConnectSnapshot(
        val connectionCount: Int,
        val authToken: String?,
        val adds: List<AddQuery>,
        val pending: List<ClientMessage>,
        val maxObservedTimestamp: Long?,
    )

    // ── Inbound message handling ────────────────────────────────────────

    private suspend fun handleMessage(text: String) {
        val message = try {
            json.decodeFromString(ServerMessage.serializer(), text)
        } catch (_: Exception) {
            return // Unknown or unsupported server message; ignore for forward-compat.
        }
        when (message) {
            is Transition -> applyTransition(message)
            is MutationResponse -> completeRequest(
                message.requestId, message.success, message.result, message.errorMessage, message.errorData,
            )
            is ActionResponse -> completeRequest(
                message.requestId, message.success, message.result, message.errorMessage, message.errorData,
            )
            is AuthError -> { /* Auth rejected; surface via connection state only. */ }
            is FatalError -> session?.close()
            Ping -> { /* Keep-alive; nothing to do. */ }
        }
    }

    private suspend fun applyTransition(transition: Transition) {
        transition.endVersion.ts?.let { ts ->
            maxObservedTimestamp = maxOf(maxObservedTimestamp ?: ts, ts)
        }
        for (modification in transition.modifications) {
            when (modification) {
                is QueryUpdated -> {
                    val state = stateMutex.withLock { queriesById[modification.queryId] }
                    state?.flow?.value = ConvexResult.Success(
                        ConvexCodec.decodeToPlainJson(modification.value ?: JsonNull),
                    )
                }
                is QueryFailed -> {
                    val state = stateMutex.withLock { queriesById[modification.queryId] }
                    state?.flow?.value = ConvexResult.Failure(
                        ConvexError(
                            message = modification.errorMessage,
                            data = modification.errorData?.let { ConvexCodec.decodeToPlainJson(it) },
                        ),
                    )
                }
                is QueryRemoved -> { /* Server dropped the query; local state already gone. */ }
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private suspend fun sendRaw(session: DefaultClientWebSocketSession, message: ClientMessage) {
        val text = json.encodeToString(ClientMessage.serializer(), message)
        sendMutex.withLock { session.send(Frame.Text(text)) }
    }

    private fun refreshInflightState() {
        _connectionState.update {
            it.copy(
                inflightMutations = inflightMutations,
                inflightActions = inflightActions,
                hasInflightRequests = inflightMutations + inflightActions > 0,
            )
        }
    }

    private fun queryToken(path: String, args: JsonObject): String = "$path|$args"

    private companion object {
        fun buildWsUrl(deploymentUrl: String, syncPath: String): String {
            val base = deploymentUrl.trimEnd('/')
            val wsBase = when {
                base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
                base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
                base.startsWith("wss://") || base.startsWith("ws://") -> base
                else -> "wss://$base"
            }
            return wsBase + (if (syncPath.startsWith("/")) syncPath else "/$syncPath")
        }
    }
}
