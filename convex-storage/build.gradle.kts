@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import io.github.androidpoet.convex.Configuration

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
}

kotlin {
    explicitApi()
    jvmToolchain(17)

    androidTarget { publishLibraryVariants("release") }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":convex-core"))
            implementation(project(":convex-client"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "io.github.androidpoet.convex.storage"
    compileSdk = Configuration.COMPILE_SDK
    defaultConfig { minSdk = Configuration.MIN_SDK }
}

mavenPublishing {
    coordinates(Configuration.GROUP, "convex-storage", Configuration.VERSION)
}
