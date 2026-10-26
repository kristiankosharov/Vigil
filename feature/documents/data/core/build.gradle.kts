plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vigil.feature.documents.data"
}

dependencies {
    implementation(projects.feature.documents.data.api)
    implementation(projects.common.core)
    implementation(projects.common.datastore)
    implementation(projects.common.security) // per-document keys: RSA (Keystore) + the vault
}
