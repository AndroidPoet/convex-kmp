package io.github.androidpoet.convex.realtime.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ── Client → Server ─────────────────────────────────────────────────────

/**
 * A message sent from the client to the Convex backend over the sync WebSocket.
 * The `"type"` field discriminates the variant (see `Json.classDiscriminator`).
 */
@Serializable
internal sealed interface ClientMessage

@Serializable
@SerialName("Connect")
internal data class Connect(
    val sessionId: String,
    val connectionCount: Int,
    val lastCloseReason: String? = null,
    val maxObservedTimestamp: Long? = null,
) : ClientMessage

@Serializable
@SerialName("ModifyQuerySet")
internal data class ModifyQuerySet(
    val baseVersion: Int,
    val newVersion: Int,
    val modifications: List<QuerySetModification>,
) : ClientMessage

@Serializable
@SerialName("Mutation")
internal data class MutationMessage(
    val requestId: Int,
    val udfPath: String,
    val args: List<JsonObject>,
) : ClientMessage

@Serializable
@SerialName("Action")
internal data class ActionMessage(
    val requestId: Int,
    val udfPath: String,
    val args: List<JsonObject>,
) : ClientMessage

@Serializable
@SerialName("Authenticate")
internal data class Authenticate(
    val baseVersion: Int,
    val tokenType: String,
    val value: String? = null,
) : ClientMessage

@Serializable
internal sealed interface QuerySetModification

@Serializable
@SerialName("Add")
internal data class AddQuery(
    val queryId: Int,
    val udfPath: String,
    val args: List<JsonObject>,
    val journal: String? = null,
    val componentPath: String? = null,
) : QuerySetModification

@Serializable
@SerialName("Remove")
internal data class RemoveQuery(
    val queryId: Int,
) : QuerySetModification

// ── Server → Client ─────────────────────────────────────────────────────

/** A message received from the Convex backend over the sync WebSocket. */
@Serializable
internal sealed interface ServerMessage

@Serializable
@SerialName("Transition")
internal data class Transition(
    val startVersion: StateVersion = StateVersion(),
    val endVersion: StateVersion = StateVersion(),
    val modifications: List<StateModification> = emptyList(),
) : ServerMessage

@Serializable
@SerialName("MutationResponse")
internal data class MutationResponse(
    val requestId: Int,
    val success: Boolean,
    val result: JsonElement? = null,
    val errorMessage: String? = null,
    val errorData: JsonElement? = null,
    val logLines: List<String> = emptyList(),
    val ts: Long? = null,
) : ServerMessage

@Serializable
@SerialName("ActionResponse")
internal data class ActionResponse(
    val requestId: Int,
    val success: Boolean,
    val result: JsonElement? = null,
    val errorMessage: String? = null,
    val errorData: JsonElement? = null,
    val logLines: List<String> = emptyList(),
) : ServerMessage

@Serializable
@SerialName("AuthError")
internal data class AuthError(
    val error: String,
    val baseVersion: Int? = null,
) : ServerMessage

@Serializable
@SerialName("FatalError")
internal data class FatalError(
    val error: String,
) : ServerMessage

@Serializable
@SerialName("Ping")
internal data object Ping : ServerMessage

@Serializable
internal data class StateVersion(
    val querySet: Int = 0,
    val ts: Long? = null,
    val identity: Int = 0,
)

@Serializable
internal sealed interface StateModification

@Serializable
@SerialName("QueryUpdated")
internal data class QueryUpdated(
    val queryId: Int,
    val value: JsonElement? = null,
    val logLines: List<String> = emptyList(),
    val journal: String? = null,
) : StateModification

@Serializable
@SerialName("QueryFailed")
internal data class QueryFailed(
    val queryId: Int,
    val errorMessage: String,
    val errorData: JsonElement? = null,
    val logLines: List<String> = emptyList(),
    val journal: String? = null,
) : StateModification

@Serializable
@SerialName("QueryRemoved")
internal data class QueryRemoved(
    val queryId: Int,
) : StateModification
