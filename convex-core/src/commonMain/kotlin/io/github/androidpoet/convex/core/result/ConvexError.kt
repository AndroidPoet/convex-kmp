package io.github.androidpoet.convex.core.result

import kotlinx.serialization.json.JsonElement

public data class ConvexError(
    val message: String,
    val data: JsonElement?,
) {
    public fun toException(): ConvexException = ConvexException(this)
}

public class ConvexException(
    public val error: ConvexError,
) : Exception(error.message)
