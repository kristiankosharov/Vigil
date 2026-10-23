plugins {
    alias(libs.plugins.vigil.jvm.library)
    alias(libs.plugins.vigil.hilt)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kotlinx.coroutines.core) // Flow is part of AppResult's and asRetryingResult's signatures
    implementation(libs.kotlinx.serialization.json)
}
