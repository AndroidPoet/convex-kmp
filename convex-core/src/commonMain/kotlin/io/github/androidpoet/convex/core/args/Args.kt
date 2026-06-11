package io.github.androidpoet.convex.core.args

import io.github.androidpoet.convex.core.values.ConvexCodec
import io.github.androidpoet.convex.core.values.ConvexValue
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

    /** Adds an `Int` as a Convex `Float64` (Convex numbers are 64-bit floats). */
    public infix fun String.to(value: Int) {
        entries[this] = JsonPrimitive(value)
    }

    /** Adds a `Long` as a Convex `Int64`, wire-encoded as `{"$integer": ...}`. */
    public infix fun String.to(value: Long) {
        entries[this] = ConvexCodec.encode(ConvexValue.Int64(value))
    }

    /** Adds a `Double` as a Convex `Float64` (non-finite values are `$float`-encoded). */
    public infix fun String.to(value: Double) {
        entries[this] = ConvexCodec.encode(ConvexValue.Float64(value))
    }

    /** Adds binary data as Convex `Bytes`, wire-encoded as `{"$bytes": ...}`. */
    public infix fun String.to(value: ByteArray) {
        entries[this] = ConvexCodec.encode(ConvexValue.Bytes(value))
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
