plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
}

android {
    namespace = "app.vigil.common.security"
}

dependencies {
    api(projects.common.core)
    api(libs.kotlinx.coroutines.core) // Vault.isOpen is a StateFlow
    implementation(libs.bouncycastle.prov)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
