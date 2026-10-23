plugins {
    alias(libs.plugins.vigil.feature.ui) // adds :common:ui
}

android {
    namespace = "app.vigil.feature.home.ui"
}

dependencies {
    implementation(projects.feature.home.domain)
    implementation(projects.common.core)
}
