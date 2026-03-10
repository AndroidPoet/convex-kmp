package io.github.androidpoet.convex.storage.di

import io.github.androidpoet.convex.storage.StorageClient
import io.github.androidpoet.convex.storage.StorageClientImpl
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

public val storageModule: Module = module {
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
    single<StorageClient> { StorageClientImpl(get(), get()) }
}
