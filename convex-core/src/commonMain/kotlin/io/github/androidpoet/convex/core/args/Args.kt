package io.github.androidpoet.convex.core.args

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@DslMarker
public annotation class ArgsDsl

@ArgsDsl
public class ArgsBuilder {
    private val entries = mutableMapOf<String, JsonElement>()

    public infix fun String.to(value: String) {
        entries[this] = JsonPrimitive(value)
    }

    public infix fun String.to(value: Int) {
        entries[this] = JsonPrimitive(value)
    }

    public infix fun String.to(value: Long) {
        entries[this] = JsonPrimitive(value)
    }

    public infix fun String.to(value: Double) {
        entries[this] = JsonPrimitive(value)
    }

    public infix fun String.to(value: Boolean) {
        entries[this] = JsonPrimitive(value)
    }

    public infix fun String.to(value: JsonElement) {
        entries[this] = value
    }

    public fun putNull(key: String) {
        entries[key] = JsonNull
    }

    public infix fun String.to(value: List<JsonElement>) {
        entries[this] = JsonArray(value)
    }

    public fun nested(key: String, block: ArgsBuilder.() -> Unit) {
        entries[key] = ArgsBuilder().apply(block).build()
    }

    public fun build(): JsonObject = JsonObject(entries.toMap())
}

public fun args(block: ArgsBuilder.() -> Unit): JsonObject =
    ArgsBuilder().apply(block).build()

public fun emptyArgs(): JsonObject = JsonObject(emptyMap())
