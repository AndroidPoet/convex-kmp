package io.github.androidpoet.convex.core.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
public data class ConvexResponse(
    val status: Status,
    val value: JsonElement? = null,
    val errorMessage: String? = null,
    val errorData: JsonElement? = null,
    val logLines: List<String> = emptyList(),
    /** Read timestamp returned by consistent-query endpoints, when present. */
    val ts: Long? = null,
) {
    @Serializable
    public enum class Status {
        @SerialName("success")
        SUCCESS,

        @SerialName("error")
        ERROR,
    }

    public val isSuccess: Boolean get() = status == Status.SUCCESS
    public val isError: Boolean get() = status == Status.ERROR
}
