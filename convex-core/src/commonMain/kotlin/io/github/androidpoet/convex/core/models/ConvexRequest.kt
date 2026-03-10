package io.github.androidpoet.convex.core.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
public data class FunctionRequest(
    val path: String,
    val args: JsonObject = JsonObject(emptyMap()),
    val format: String = "json",
)

@Serializable
public data class PaginationOpts(
    val numItems: Int,
    val cursor: String? = null,
)

@Serializable
public data class UploadResponse(
    val storageId: String,
)

@Serializable
public data class PaginationResult(
    val page: List<JsonElement>,
    val continueCursor: String,
    val isDone: Boolean,
)
