package io.github.androidpoet.convex.realtime

import io.github.androidpoet.convex.client.ConvexClient

public fun createRealtimeClient(client: ConvexClient): RealtimeClient =
    PollingRealtimeClient(client)
