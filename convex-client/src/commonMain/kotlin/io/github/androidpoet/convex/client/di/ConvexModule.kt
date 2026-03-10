package io.github.androidpoet.convex.client.di

import io.github.androidpoet.convex.client.ConvexClient
import io.github.androidpoet.convex.client.ConvexClientImpl
import io.github.androidpoet.convex.client.ConvexConfig
import io.github.androidpoet.convex.client.transport.HttpTransport
import io.github.androidpoet.convex.client.transport.platformEngine
import org.koin.core.module.Module
import org.koin.dsl.module

public fun convexModule(config: ConvexConfig): Module = module {
    single<HttpTransport> { HttpTransport(config, platformEngine()) }
    single<ConvexClient> { ConvexClientImpl(get()) }
}
