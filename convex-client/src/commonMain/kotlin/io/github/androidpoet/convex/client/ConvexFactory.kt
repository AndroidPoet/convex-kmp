package io.github.androidpoet.convex.client

import io.github.androidpoet.convex.client.transport.HttpTransport
import io.github.androidpoet.convex.client.transport.platformEngine

public fun createConvexClient(config: ConvexConfig): ConvexClient =
    ConvexClientImpl(
        transport = HttpTransport(
            config = config,
            engine = platformEngine(),
        ),
    )
