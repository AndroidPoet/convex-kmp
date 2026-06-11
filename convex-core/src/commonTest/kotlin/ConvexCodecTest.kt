import io.github.androidpoet.convex.core.values.ConvexCodec
import io.github.androidpoet.convex.core.values.ConvexValue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConvexCodecTest {

    private fun roundTrip(value: ConvexValue): ConvexValue =
        ConvexCodec.decode(ConvexCodec.encode(value))

    @Test
    fun int64_encodes_as_little_endian_integer() {
        // 1L little-endian 8 bytes = 01 00 00 00 00 00 00 00 -> base64 "AQAAAAAAAAA="
        val encoded = ConvexCodec.encode(ConvexValue.Int64(1L)).jsonObject
        assertEquals("AQAAAAAAAAA=", encoded.getValue("\$integer").jsonPrimitive.content)
    }

    @Test
    fun int64_round_trips_including_extremes() {
        for (v in listOf(0L, 1L, -1L, 42L, Long.MAX_VALUE, Long.MIN_VALUE, 9007199254740993L)) {
            assertEquals(ConvexValue.Int64(v), roundTrip(ConvexValue.Int64(v)))
        }
    }

    @Test
    fun float64_is_a_bare_number() {
        val encoded = ConvexCodec.encode(ConvexValue.Float64(3.5))
        assertEquals(3.5, encoded.jsonPrimitive.double)
        assertEquals(ConvexValue.Float64(3.5), roundTrip(ConvexValue.Float64(3.5)))
    }

    @Test
    fun non_finite_floats_use_dollar_float() {
        for (v in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val encoded = ConvexCodec.encode(ConvexValue.Float64(v))
            assertTrue(encoded is JsonObject && encoded.containsKey("\$float"))
            val decoded = ConvexCodec.decode(encoded) as ConvexValue.Float64
            assertEquals(v.toRawBits(), decoded.value.toRawBits())
        }
    }

    @Test
    fun negative_zero_uses_dollar_float() {
        val encoded = ConvexCodec.encode(ConvexValue.Float64(-0.0))
        assertTrue(encoded is JsonObject && encoded.containsKey("\$float"))
        val decoded = ConvexCodec.decode(encoded) as ConvexValue.Float64
        assertEquals((-0.0).toRawBits(), decoded.value.toRawBits())
    }

    @Test
    fun bytes_round_trip_as_base64() {
        val bytes = byteArrayOf(0, 1, 2, 127, -128, -1)
        val encoded = ConvexCodec.encode(ConvexValue.Bytes(bytes)).jsonObject
        assertTrue(encoded.containsKey("\$bytes"))
        val decoded = ConvexCodec.decode(encoded) as ConvexValue.Bytes
        assertTrue(bytes.contentEquals(decoded.value))
    }

    @Test
    fun strings_and_ids_are_bare_strings() {
        val encoded = ConvexCodec.encode(ConvexValue.Str("j57abc"))
        assertEquals("j57abc", encoded.jsonPrimitive.content)
        assertTrue(encoded.jsonPrimitive.isString)
    }

    @Test
    fun bare_number_decodes_as_float64_not_int64() {
        val decoded = ConvexCodec.decode(JsonPrimitive(5))
        assertEquals(ConvexValue.Float64(5.0), decoded)
    }

    @Test
    fun nested_objects_and_arrays_round_trip() {
        val value = ConvexValue.Obj(
            mapOf(
                "id" to ConvexValue.Str("abc"),
                "count" to ConvexValue.Int64(100L),
                "ratio" to ConvexValue.Float64(0.25),
                "tags" to ConvexValue.Arr(listOf(ConvexValue.Str("a"), ConvexValue.Bool(true))),
                "missing" to ConvexValue.Null,
            ),
        )
        assertEquals(value, roundTrip(value))
    }

    @Test
    fun decodeToPlainJson_unwraps_special_forms() {
        val wire = buildJsonObject {
            put("count", ConvexCodec.encode(ConvexValue.Int64(7L)))
            put("ratio", ConvexCodec.encode(ConvexValue.Float64(1.5)))
        }
        val plain = ConvexCodec.decodeToPlainJson(wire).jsonObject
        assertEquals(7L, plain.getValue("count").jsonPrimitive.long)
        assertEquals(1.5, plain.getValue("ratio").jsonPrimitive.double)
    }

    @Test
    fun decodeToPlainJson_maps_undefined_to_null() {
        val wire = buildJsonObject {
            put("x", JsonObject(mapOf("\$undefined" to JsonPrimitive(true))))
        }
        val plain = ConvexCodec.decodeToPlainJson(wire).jsonObject
        assertTrue(plain.getValue("x") is kotlinx.serialization.json.JsonNull)
    }

    @Test
    fun decodeToPlainJson_passes_plain_through() {
        val plain = buildJsonObject { put("name", JsonPrimitive("convex")) }
        val result = ConvexCodec.decodeToPlainJson(plain).jsonObject
        assertEquals("convex", result.getValue("name").jsonPrimitive.content)
        assertEquals(plain, result)
    }
}
