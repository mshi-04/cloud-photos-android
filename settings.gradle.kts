pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Cloud Photos"
include(":app")

// Core modules
include(":core:common")
include(":core:data")
include(":core:ui")

// Feature modules
include(":feature:auth:domain")
include(":feature:auth:data")
include(":feature:auth:ui")
include(":feature:media:domain")
include(":feature:media:data")
include(":feature:media:ui")
include(":feature:settings:domain")
include(":feature:settings:data")
