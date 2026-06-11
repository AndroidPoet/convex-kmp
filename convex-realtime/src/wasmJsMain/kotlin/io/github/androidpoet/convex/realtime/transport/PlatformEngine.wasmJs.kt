package io.github.androidpoet.convex.realtime.transport

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

internal actual fun platformEngine(): HttpClientEngineFactory<*> = Js
