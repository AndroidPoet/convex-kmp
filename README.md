<p align="center">
  <img src="art/logo.jpeg" width="720" alt="Convex KMP Logo">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.1.10-blue.svg?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Ktor-3.1.1-blue.svg" alt="Ktor">
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20JVM%20%7C%20WasmJs-green.svg" alt="Platforms">
  <img src="https://img.shields.io/badge/Maven%20Central-0.1.0-blue.svg" alt="Maven Central">
  <img src="https://img.shields.io/badge/License-MIT-orange.svg" alt="License">
</p>

# Convex KMP

Kotlin Multiplatform SDK for [Convex](https://convex.dev) — type-safe, coroutine-first, modular backend client for every platform Kotlin runs on.

## Features

- **Multiplatform** — Android, iOS, JVM Desktop, and WasmJs from a single codebase
- **Type-safe functions** — Call Convex queries, mutations, and actions with compile-time safety
- **Result monad** — `ConvexResult<T>` with `map`, `flatMap`, `recover` — no exceptions leak to callers
- **Args DSL** — Build function arguments with a clean Kotlin DSL instead of raw JSON
- **Modular architecture** — Pick only the modules you need: core, client, storage, realtime
- **Simple wiring** — Factory functions with no DI framework required
- **File storage** — Three-step upload flow, download URLs, and deletion out of the box
- **Reactive subscriptions** — Observe query results as Kotlin Flows with automatic polling
- **Dual auth modes** — Bearer tokens (end-user JWT) and admin auth (deploy keys)
- **Value-class IDs** — `DocumentId`, `StorageId`, `FunctionReference` prevent mixups at compile time

## Setup

Add the dependencies you need to your `build.gradle.kts`:

```kotlin
// Version catalog (gradle/libs.versions.toml)
[versions]
convex = "0.1.0"

[libraries]
convex-core = { module = "io.github.androidpoet:convex-core", version.ref = "convex" }
convex-client = { module = "io.github.androidpoet:convex-client", version.ref = "convex" }
convex-storage = { module = "io.github.androidpoet:convex-storage", version.ref = "convex" }
convex-realtime = { module = "io.github.androidpoet:convex-realtime", version.ref = "convex" }
```

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.convex.client)   // includes convex-core
            implementation(libs.convex.storage)  // optional
            implementation(libs.convex.realtime) // optional
        }
    }
}
```

## Usage

### Create a Client

```kotlin
// Direct instantiation
val convex = Convex.create("https://your-app.convex.cloud") {
    logging = true
}

val convex = createConvexClient(
    ConvexConfig(DeploymentUrl("https://your-app.convex.cloud")),
)
val storage = createStorageClient(convex)
val realtime = createRealtimeClient(convex)
```

### Authentication

```kotlin
// End-user JWT (from Clerk, Auth0, etc.)
convex.setAuth("eyJhbG...")

// Admin access with deploy key
convex.setAdminAuth("prod:deploy-key-abc123")

// Clear auth
convex.clearAuth()
```

### Queries, Mutations & Actions

```kotlin
// Query with args DSL
val messages = convex.query("messages:list", args {
    "channel" to "#general"
    "limit" to 50
})

messages.onSuccess { data ->
    println("Got messages: $data")
}.onFailure { error ->
    println("Error: ${error.message}")
}

// Mutation
val id = convex.mutation("messages:send", args {
    "body" to "Hello from Kotlin!"
    "channel" to "#general"
})

// Action (external API calls)
val result = convex.action("actions:sendEmail", args {
    "to" to "user@example.com"
    "subject" to "Welcome"
})
```

### Typed Results

```kotlin
@Serializable
data class Message(
    val _id: String,
    val body: String,
    val channel: String,
    val _creationTime: Double,
)

val messages: ConvexResult<List<Message>> = convex.queryTyped("messages:list")
```

### Pagination

```kotlin
val page = convex.queryPaginated(
    path = "messages:list",
    args = args { "channel" to "#general" },
    paginationOpts = PaginationOpts(numItems = 25, cursor = null),
)

page.onSuccess { result ->
    println("Page: ${result.page}")
    println("Has more: ${!result.isDone}")
    println("Next cursor: ${result.continueCursor}")
}
```

### File Storage

```kotlin
val storage: StorageClient by inject()

// One-step upload (generates URL + uploads)
val storageId = storage.uploadFile(
    data = imageBytes,
    contentType = "image/png",
)

// Get download URL
val url = storage.getUrl(storageId = StorageId("storage_abc123"))

// Delete
storage.deleteFile(storageId = StorageId("storage_abc123"))
```

### Realtime Subscriptions

```kotlin
val realtime: RealtimeClient by inject()

realtime.subscribe("messages:list", args {
    "channel" to "#general"
}).collect { result ->
    result.onSuccess { messages ->
        // UI updates automatically
    }
}
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Your App                           │
├──────────┬──────────────┬──────────────┬────────────────┤
│ convex-  │   convex-    │   convex-    │    convex-     │
│ realtime │   storage    │   client     │    core        │
│          │              │              │                │
│ Polling  │ Upload Flow  │ ConvexClient │ ConvexResult   │
│ Flow     │ Download URL │ HttpTransport│ Args DSL       │
│ Subscribe│ Delete       │ Auth State   │ Value IDs      │
│          │              │ Factory API  │ Models         │
├──────────┴──────────────┼──────────────┤ Serialization  │
│                         │   Ktor       │                │
│                         │   OkHttp     │                │
│                         │   Darwin     │                │
│                         │   Js         │                │
└─────────────────────────┴──────────────┴────────────────┘
```

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| **convex-core** | `io.github.androidpoet:convex-core` | Value types, result monad, args DSL, serialization models |
| **convex-client** | `io.github.androidpoet:convex-client` | HTTP transport, query/mutation/action execution, auth |
| **convex-storage** | `io.github.androidpoet:convex-storage` | File upload (3-step flow), download URLs, deletion |
| **convex-realtime** | `io.github.androidpoet:convex-realtime` | Reactive query subscriptions as Kotlin Flows |

## Targets

| Platform | Target | Ktor Engine |
|----------|--------|-------------|
| Android | `androidTarget()` | OkHttp |
| iOS | `iosX64()` `iosArm64()` `iosSimulatorArm64()` | Darwin |
| Desktop | `jvm()` | OkHttp |
| Web | `wasmJs()` | Js |

## Tech Stack

| Layer | Library |
|-------|---------|
| Language | [Kotlin 2.1.10](https://kotlinlang.org/) |
| Networking | [Ktor 3.1.1](https://ktor.io/) |
| Serialization | [kotlinx.serialization 1.8.0](https://github.com/Kotlin/kotlinx.serialization) |
| Coroutines | [kotlinx.coroutines 1.10.1](https://github.com/Kotlin/kotlinx.coroutines) |
| Date/Time | [kotlinx-datetime 0.6.2](https://github.com/Kotlin/kotlinx-datetime) |
| Publishing | [vanniktech maven-publish 0.30.0](https://github.com/vanniktech/gradle-maven-publish-plugin) |

## Build

```bash
# Compile all targets
./gradlew compileKotlinJvm

# Run tests
./gradlew jvmTest

# Publish to Maven Central (CI only)
./gradlew publishAllPublicationsToMavenCentral --no-configuration-cache
```

## License

```
MIT License

Copyright (c) 2026 Ranbir Singh

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
