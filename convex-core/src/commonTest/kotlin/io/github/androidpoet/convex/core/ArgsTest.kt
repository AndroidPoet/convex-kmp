package io.github.androidpoet.convex.core

import io.github.androidpoet.convex.core.args.args
import io.github.androidpoet.convex.core.args.emptyArgs
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArgsTest {

    @Test
    fun args_buildsJsonObject() {
        val result = args {
            "name" to "Alice"
            "age" to 30
            "active" to true
        }

        assertEquals("Alice", result["name"]?.jsonPrimitive?.content)
        assertEquals("30", result["age"]?.jsonPrimitive?.content)
        assertEquals("true", result["active"]?.jsonPrimitive?.content)
    }

    @Test
    fun args_supportsNull() {
        val result = args {
            "value" to JsonNull
            putNull("other")
        }

        assertEquals(JsonNull, result["value"])
        assertEquals(JsonNull, result["other"])
    }

    @Test
    fun args_supportsNested() {
        val result = args {
            "user" to JsonPrimitive("test")
            nested("filter") {
                "status" to "active"
                "count" to 5
            }
        }

        val filter = result["filter"]?.jsonObject
        assertEquals("active", filter?.get("status")?.jsonPrimitive?.content)
    }

    @Test
    fun emptyArgs_returnsEmptyObject() {
        val result = emptyArgs()
        assertTrue(result.isEmpty())
    }
}
