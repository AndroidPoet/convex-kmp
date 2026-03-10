package io.github.androidpoet.convex.client

import io.github.androidpoet.convex.client.transport.HttpTransport
import io.github.androidpoet.convex.client.transport.platformEngine

public object Convex {

    public fun create(
        deploymentUrl: String,
        configure: ConvexConfigBuilder.() -> Unit = {},
    ): ConvexClient {
        val config = ConvexConfigBuilder(deploymentUrl).apply(configure).build()
        val transport = HttpTransport(config, platformEngine())
        return ConvexClientImpl(transport)
    }
}
