package io.github.androidpoet.convex.client

import io.github.androidpoet.convex.core.types.DeploymentUrl

@DslMarker
public annotation class ConvexConfigDsl

@ConvexConfigDsl
public class ConvexConfigBuilder internal constructor(
    private val deploymentUrl: String,
) {
    public var logging: Boolean = false
    public var logLevel: LogLevel = LogLevel.INFO

    public fun build(): ConvexConfig = ConvexConfig(
        deploymentUrl = DeploymentUrl(deploymentUrl),
        logging = logging,
        logLevel = logLevel,
    )
}

public data class ConvexConfig(
    val deploymentUrl: DeploymentUrl,
    val logging: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
)

public enum class LogLevel {
    ALL,
    HEADERS,
    BODY,
    INFO,
    NONE,
}
