package io.github.androidpoet.convex.core.values

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Encodes and decodes [ConvexValue]s to and from the Convex `convex_encoded_json`
 * wire format used by the JavaScript SDK (`convexToJson` / `jsonToConvex`).
 *
 * Wire representations (the `$`-prefixed forms are the parts plain JSON cannot express):
 * - [ConvexValue.Int64]   -> `{ "$integer": "<base64 of 8 little-endian bytes>" }`
 * - [ConvexValue.Float64] -> a bare JSON number, **except** non-finite values and
 *   negative zero, which become `{ "$float": "<base64 of 8 little-endian bytes>" }`
 * - [ConvexValue.Bytes]   -> `{ "$bytes": "<standard base64>" }`
 * - everything else maps directly to its JSON counterpart.
 *
 * On decode, a bare JSON number is always a [ConvexValue.Float64]; integers only
 * arrive via the `$integer` wrapper, exactly as in the JS protocol.
 */
@OptIn(ExperimentalEncodingApi::class)
public object ConvexCodec {
    private const val KEY_INTEGER = "\$integer"
    private const val KEY_FLOAT = "\$float"
    private const val KEY_BYTES = "\$bytes"
    private const val KEY_UNDEFINED = "\$undefined"

    /** Encodes a [ConvexValue] into its `convex_encoded_json` [JsonElement]. */
    public fun encode(value: ConvexValue): JsonElement =
        when (value) {
            is ConvexValue.Null -> JsonNull
            is ConvexValue.Bool -> JsonPrimitive(value.value)
            is ConvexValue.Str -> JsonPrimitive(value.value)
            is ConvexValue.Int64 -> wrap(KEY_INTEGER, Base64.encode(longToLeBytes(value.value)))
            is ConvexValue.Float64 -> encodeFloat(value.value)
            is ConvexValue.Bytes -> wrap(KEY_BYTES, Base64.encode(value.value))
            is ConvexValue.Arr -> JsonArray(value.value.map { encode(it) })
            is ConvexValue.Obj -> JsonObject(value.value.mapValues { encode(it.value) })
        }

    /** Decodes a `convex_encoded_json` [JsonElement] into a [ConvexValue]. */
    public fun decode(json: JsonElement): ConvexValue =
        when (json) {
            is JsonNull -> ConvexValue.Null
            is JsonArray -> ConvexValue.Arr(json.map { decode(it) })
            is JsonObject -> decodeObject(json)
            is JsonPrimitive -> decodePrimitive(json)
        }

    /**
     * Recursively rewrites a `convex_encoded_json` [JsonElement] into plain JSON,
     * unwrapping the `$`-prefixed forms so that ordinary deserializers can read it:
     * `$integer` -> JSON number, `$float` -> JSON number, `$bytes` -> base64 string,
     * `$undefined` -> `null`. Plain JSON passes through unchanged.
     */
    public fun decodeToPlainJson(json: JsonElement): JsonElement =
        when (json) {
            is JsonNull -> JsonNull
            is JsonArray -> JsonArray(json.map { decodeToPlainJson(it) })
            is JsonObject ->
                when {
                    json.containsSpecialKey(KEY_INTEGER) ->
                        JsonPrimitive(leBytesToLong(Base64.decode(json.specialString(KEY_INTEGER))))
                    json.containsSpecialKey(KEY_FLOAT) ->
                        JsonPrimitive(leBytesToDouble(Base64.decode(json.specialString(KEY_FLOAT))))
                    json.containsSpecialKey(KEY_BYTES) ->
                        JsonPrimitive(json.specialString(KEY_BYTES))
                    json.containsSpecialKey(KEY_UNDEFINED) -> JsonNull
                    else -> JsonObject(json.mapValues { decodeToPlainJson(it.value) })
                }
            is JsonPrimitive -> json
        }

    private fun encodeFloat(d: Double): JsonElement =
        if (d.isFinite() && !isNegativeZero(d)) {
            JsonPrimitive(d)
        } else {
            wrap(KEY_FLOAT, Base64.encode(longToLeBytes(d.toRawBits())))
        }

    private fun decodeObject(json: JsonObject): ConvexValue =
        when {
            json.containsSpecialKey(KEY_INTEGER) ->
                ConvexValue.Int64(leBytesToLong(Base64.decode(json.specialString(KEY_INTEGER))))
            json.containsSpecialKey(KEY_FLOAT) ->
                ConvexValue.Float64(leBytesToDouble(Base64.decode(json.specialString(KEY_FLOAT))))
            json.containsSpecialKey(KEY_BYTES) ->
                ConvexValue.Bytes(Base64.decode(json.specialString(KEY_BYTES)))
            json.containsSpecialKey(KEY_UNDEFINED) -> ConvexValue.Null
            else -> ConvexValue.Obj(json.mapValues { decode(it.value) })
        }

    private fun decodePrimitive(primitive: JsonPrimitive): ConvexValue =
        when {
            primitive.isString -> ConvexValue.Str(primitive.content)
            primitive.booleanOrNull != null -> ConvexValue.Bool(primitive.boolean)
            // Bare JSON numbers are always Float64 on the Convex wire; Int64 only
            // ever arrives wrapped in `$integer`.
            else -> ConvexValue.Float64(primitive.double)
        }

    private fun wrap(key: String, base64: String): JsonObject =
        JsonObject(mapOf(key to JsonPrimitive(base64)))

    private fun JsonObject.containsSpecialKey(key: String): Boolean =
        size == 1 && containsKey(key)

    private fun JsonObject.specialString(key: String): String =
        getValue(key).jsonPrimitive.content

    private fun isNegativeZero(d: Double): Boolean = d == 0.0 && d.toRawBits() != 0L

    // ── 64-bit little-endian helpers ────────────────────────────────────

    private fun longToLeBytes(value: Long): ByteArray {
        val bytes = ByteArray(8)
        var v = value
        for (i in 0 until 8) {
            bytes[i] = (v and 0xFF).toByte()
            v = v ushr 8
        }
        return bytes
    }

    private fun leBytesToLong(bytes: ByteArray): Long {
        require(bytes.size == 8) { "Expected 8 bytes for a 64-bit value, got ${bytes.size}" }
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((bytes[i].toLong() and 0xFF) shl (8 * i))
        }
        return result
    }

    private fun leBytesToDouble(bytes: ByteArray): Double = Double.fromBits(leBytesToLong(bytes))
}

/** Encodes this [ConvexValue] into its `convex_encoded_json` [JsonElement]. */
public fun ConvexValue.toJsonElement(): JsonElement = ConvexCodec.encode(this)

/** Decodes this `convex_encoded_json` [JsonElement] into a [ConvexValue]. */
public fun JsonElement.toConvexValue(): ConvexValue = ConvexCodec.decode(this)
