plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
}

android {
    namespace = "app.vigil.feature.documents.data"
}

dependencies {
    implementation(projects.feature.documents.data.api)
    implementation(projects.common.core)
    implementation(projects.common.database)
}
