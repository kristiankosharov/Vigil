plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
}

android {
    namespace = "app.vigil.common.datastore"
}

dependencies {
    api(projects.common.core) // AppResult / AppError are part of this module's signatures
    api(libs.androidx.datastore) // features get DataStore<T> from encryptedDataStore
    api(libs.tink.android) // features inject the @DataStoreAead Aead
    api(libs.kotlinx.serialization.json) // encryptedDataStore takes a KSerializer
    implementation(libs.androidx.datastore.tink)
    implementation(libs.timber)
}
