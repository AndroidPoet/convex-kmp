package io.github.androidpoet.convex.client.auth

public sealed interface AuthState {
    public data object None : AuthState
    public data class Bearer(val token: String) : AuthState
    public data class Admin(val deployKey: String) : AuthState

    public fun headerValue(): String? = when (this) {
        is None -> null
        is Bearer -> "Bearer $token"
        is Admin -> "Convex $deployKey"
    }
}
