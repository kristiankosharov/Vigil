plugins {
    `kotlin-dsl`
}

group = "app.vigil.buildlogic"

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.room3.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "vigil.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "vigil.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "vigil.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("jvmLibrary") {
            id = "vigil.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("hilt") {
            id = "vigil.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("room") {
            id = "vigil.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("featureUi") {
            id = "vigil.feature.ui"
            implementationClass = "FeatureUiConventionPlugin"
        }
        register("testing") {
            id = "vigil.testing"
            implementationClass = "TestingConventionPlugin"
        }
        register("environment") {
            id = "vigil.environment"
            implementationClass = "EnvironmentConventionPlugin"
        }
        register("detekt") {
            id = "vigil.detekt"
            implementationClass = "DetektConventionPlugin"
        }
    }
}
