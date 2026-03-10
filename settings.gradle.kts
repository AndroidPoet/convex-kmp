pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "convex-kmp"

include(":convex-core")
include(":convex-client")
include(":convex-storage")
include(":convex-realtime")
