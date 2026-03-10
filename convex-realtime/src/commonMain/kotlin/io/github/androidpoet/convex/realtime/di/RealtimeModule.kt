package io.github.androidpoet.convex.realtime.di

import io.github.androidpoet.convex.realtime.PollingRealtimeClient
import io.github.androidpoet.convex.realtime.RealtimeClient
import org.koin.core.module.Module
import org.koin.dsl.module

public val realtimeModule: Module = module {
    single<RealtimeClient> { PollingRealtimeClient(get()) }
}
