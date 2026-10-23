plugins {
    alias(libs.plugins.vigil.feature.ui) // adds :common:ui
}

android {
    namespace = "app.vigil.feature.auth.ui"
}

dependencies {
    implementation(projects.feature.auth.domain)
    implementation(projects.common.core)
}
