package io.github.androidpoet.convex.core.types

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
public value class DocumentId(public val value: String) {
    override fun toString(): String = value
}

@JvmInline
@Serializable
public value class StorageId(public val value: String) {
    override fun toString(): String = value
}

@JvmInline
@Serializable
public value class FunctionReference(public val value: String) {
    override fun toString(): String = value

    public companion object {
        public fun of(module: String, export: String): FunctionReference =
            FunctionReference("$module:$export")
    }
}

@JvmInline
@Serializable
public value class DeploymentUrl(public val value: String) {
    public val siteUrl: String
        get() = value.replace(".convex.cloud", ".convex.site")

    override fun toString(): String = value
}
