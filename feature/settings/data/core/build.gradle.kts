plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vigil.feature.settings.data"
}

dependencies {
    implementation(projects.feature.settings.data.api)
    implementation(projects.common.core)
    implementation(projects.common.datastore)
}
