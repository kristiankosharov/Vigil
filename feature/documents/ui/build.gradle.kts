plugins {
    alias(libs.plugins.vigil.feature.ui) // adds :common:ui
}

android {
    namespace = "app.vigil.feature.documents.ui"
}

dependencies {
    implementation(projects.feature.documents.domain)
    implementation(projects.common.core)
    implementation(libs.androidx.activity.compose) // BackHandler closes search
}
