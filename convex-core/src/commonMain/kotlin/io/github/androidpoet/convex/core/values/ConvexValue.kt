package io.github.androidpoet.convex.core.values

/**
 * A Convex value, mirroring the JavaScript SDK's `Value` union.
 *
 * The Convex wire protocol distinguishes 64-bit integers ([Int64]) from
 * 64-bit floats ([Float64]) and carries binary data ([Bytes]) — distinctions
 * that plain JSON cannot represent. [ConvexValue] models these faithfully so
 * values round-trip exactly the way they do in `convex/values`.
 *
 * JS mapping:
 * - `null`        -> [Null]
 * - `boolean`     -> [Bool]
 * - `number`      -> [Float64]
 * - `bigint`      -> [Int64]
 * - `string`      -> [Str] (Convex `Id`s are plain strings)
 * - `ArrayBuffer` -> [Bytes]
 * - `Array`       -> [Arr]
 * - `Object`      -> [Obj]
 */
public sealed interface ConvexValue {
    public data object Null : ConvexValue

    public data class Bool(
        public val value: Boolean,
    ) : ConvexValue

    /** A 64-bit IEEE-754 float — the JS `number` type. */
    public data class Float64(
        public val value: Double,
    ) : ConvexValue

    /** A signed 64-bit integer — the JS `bigint` / Convex `Int64` type. */
    public data class Int64(
        public val value: Long,
    ) : ConvexValue

    public data class Str(
        public val value: String,
    ) : ConvexValue

    /** Raw binary data — the JS `ArrayBuffer` / Convex `Bytes` type. */
    public class Bytes(
        public val value: ByteArray,
    ) : ConvexValue {
        override fun equals(other: Any?): Boolean =
            this === other || (other is Bytes && value.contentEquals(other.value))

        override fun hashCode(): Int = value.contentHashCode()

        override fun toString(): String = "Bytes(size=${value.size})"
    }

    public data class Arr(
        public val value: List<ConvexValue>,
    ) : ConvexValue

    public data class Obj(
        public val value: Map<String, ConvexValue>,
    ) : ConvexValue

    public companion object {
        /** The minimum representable [Int64], matching `MIN_INT64` in `convex/values`. */
        public const val MIN_INT64: Long = Long.MIN_VALUE

        /** The maximum representable [Int64], matching `MAX_INT64` in `convex/values`. */
        public const val MAX_INT64: Long = Long.MAX_VALUE
    }
}
