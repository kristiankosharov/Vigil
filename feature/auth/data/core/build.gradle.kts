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
    implementation(projects.common.security) // the tokens are sealed with the vault key

    // Firebase Auth: the only Firebase use outside :app (rules/app.md, Firebase).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services) // Task.await()
    implementation(libs.timber)
}
