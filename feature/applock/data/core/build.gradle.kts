plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vigil.feature.applock.data"
}

dependencies {
    implementation(projects.feature.applock.data.api)
    implementation(projects.common.core)
    implementation(projects.common.datastore)
    implementation(libs.androidx.biometric) // BiometricManager: is a strong biometric enrolled?
}
