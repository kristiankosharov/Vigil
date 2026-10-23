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
include(":common:ui")
include(":common:testing")

include(":feature:auth:data:api")
include(":feature:auth:data:core")
include(":feature:auth:domain")
include(":feature:auth:ui")

include(":feature:home:data:api")
include(":feature:home:data:core")
include(":feature:home:domain")
include(":feature:home:ui")

include(":konsist")
