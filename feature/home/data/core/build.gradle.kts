plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
}

android {
    namespace = "app.vigil.feature.home.data"
}

dependencies {
    implementation(projects.feature.home.data.api)
    implementation(projects.common.core)
    implementation(projects.common.database)
    // TODO(home): add projects.common.network with the API refresh.
}
