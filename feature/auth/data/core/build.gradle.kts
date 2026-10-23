plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vigil.feature.auth.data"
}

dependencies {
    implementation(projects.feature.auth.data.api)
    implementation(projects.common.core)
    implementation(projects.common.network)
    implementation(projects.common.datastore)
}
