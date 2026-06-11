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

/**
 * Builds a [FunctionReference] from a `"module:export"` path string, mirroring
 * `makeFunctionReference` in `convex/server`.
 */
public fun makeFunctionReference(name: String): FunctionReference = FunctionReference(name)

/**
 * Returns the `"module:export"` path string of a [FunctionReference], mirroring
 * `getFunctionName` in `convex/server`.
 */
public fun getFunctionName(reference: FunctionReference): String = reference.value

@JvmInline
@Serializable
public value class DeploymentUrl(public val value: String) {
    public val siteUrl: String
        get() = value.replace(".convex.cloud", ".convex.site")

    override fun toString(): String = value
}
