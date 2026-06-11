import io.github.androidpoet.convex.realtime.protocol.AddQuery
import io.github.androidpoet.convex.realtime.protocol.ClientMessage
import io.github.androidpoet.convex.realtime.protocol.Connect
import io.github.androidpoet.convex.realtime.protocol.ModifyQuerySet
import io.github.androidpoet.convex.realtime.protocol.MutationMessage
import io.github.androidpoet.convex.realtime.protocol.QueryUpdated
import io.github.androidpoet.convex.realtime.protocol.ServerMessage
import io.github.androidpoet.convex.realtime.protocol.Transition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncMessagesTest {

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun connect_serializes_with_type_discriminator() {
        val text = json.encodeToString(ClientMessage.serializer(), Connect("sess-1", 0))
        val obj = json.parseToJsonElement(text).jsonObject
        assertEquals("Connect", obj.getValue("type").jsonPrimitive.content)
        assertEquals("sess-1", obj.getValue("sessionId").jsonPrimitive.content)
    }

    @Test
    fun modify_query_set_carries_add_modifications() {
        val message: ClientMessage = ModifyQuerySet(
            baseVersion = 0,
            newVersion = 1,
            modifications = listOf(
                AddQuery(queryId = 0, udfPath = "messages:list", args = listOf(JsonObject(emptyMap()))),
            ),
        )
        val obj = json.parseToJsonElement(json.encodeToString(ClientMessage.serializer(), message)).jsonObject
        assertEquals("ModifyQuerySet", obj.getValue("type").jsonPrimitive.content)
        val mod = obj.getValue("modifications").let { (it as kotlinx.serialization.json.JsonArray)[0] }.jsonObject
        assertEquals("Add", mod.getValue("type").jsonPrimitive.content)
        assertEquals("messages:list", mod.getValue("udfPath").jsonPrimitive.content)
    }

    @Test
    fun mutation_message_wraps_args_in_array() {
        val message: ClientMessage = MutationMessage(
            requestId = 7,
            udfPath = "messages:send",
            args = listOf(JsonObject(mapOf("body" to JsonPrimitive("hi")))),
        )
        val obj = json.parseToJsonElement(json.encodeToString(ClientMessage.serializer(), message)).jsonObject
        assertEquals("Mutation", obj.getValue("type").jsonPrimitive.content)
        val args = obj.getValue("args") as kotlinx.serialization.json.JsonArray
        assertEquals(1, args.size)
        assertEquals("hi", args[0].jsonObject.getValue("body").jsonPrimitive.content)
    }

    @Test
    fun transition_deserializes_query_updated() {
        val wire = """
            {"type":"Transition",
             "startVersion":{"querySet":0,"ts":0,"identity":0},
             "endVersion":{"querySet":1,"ts":1000,"identity":0},
             "modifications":[{"type":"QueryUpdated","queryId":0,"value":{"count":5},"logLines":[]}]}
        """.trimIndent()
        val message = json.decodeFromString(ServerMessage.serializer(), wire)
        assertTrue(message is Transition)
        val mod = message.modifications.single()
        assertTrue(mod is QueryUpdated)
        assertEquals(0, mod.queryId)
        assertEquals(5, mod.value!!.jsonObject.getValue("count").jsonPrimitive.content.toInt())
    }

    @Test
    fun unknown_server_message_type_is_decodable_or_ignorable() {
        // Forward-compat: an unmodeled message type must not crash the decoder path.
        val wire = """{"type":"SomeFutureMessage","foo":1}"""
        val result = runCatching { json.decodeFromString(ServerMessage.serializer(), wire) }
        assertTrue(result.isFailure, "Unknown types are expected to fail decode and be ignored by the client")
    }
}
