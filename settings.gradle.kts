pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Downloads missing JDKs for toolchains and the Gradle daemon (gradle/gradle-daemon-jvm.properties).
    // The version catalog isn't available in this block.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Vigil"

include(":app")

include(":common:core")
include(":common:network")
include(":common:database")
include(":common:datastore")
include(":common:security")
include(":common:ui")
include(":common:testing")

include(":feature:applock:data:api")
include(":feature:applock:data:core")
include(":feature:applock:domain")
include(":feature:applock:ui")

include(":feature:auth:data:api")
include(":feature:auth:data:core")
include(":feature:auth:domain")
include(":feature:auth:ui")

include(":feature:documents:data:api")
include(":feature:documents:data:core")
include(":feature:documents:domain")
include(":feature:documents:ui")
include(":feature:settings:data:api")
include(":feature:settings:data:core")
include(":feature:settings:domain")
include(":feature:settings:ui")

include(":konsist")
