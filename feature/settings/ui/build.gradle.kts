plugins {
    alias(libs.plugins.vigil.feature.ui) // adds :common:ui
}

android {
    namespace = "app.vigil.feature.settings.ui"
}

dependencies {
    implementation(projects.feature.settings.domain)
    implementation(projects.common.core)
}
