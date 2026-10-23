plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
    alias(libs.plugins.vigil.room)
}

android {
    namespace = "app.vigil.common.database"
}

dependencies {
    api(projects.common.core) // AppResult / AppError are part of this module's signatures
    implementation(libs.timber)
}
