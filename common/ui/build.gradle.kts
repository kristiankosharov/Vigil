plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.android.compose)
}

android {
    namespace = "app.vigil.common.ui"
}

dependencies {
    api(projects.common.core) // AppError.toUiText()
    api(libs.androidx.lifecycle.viewmodel) // BaseViewModel extends ViewModel
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.activity.compose) // LocalActivity for SecurityTheme's system bars
    implementation(libs.androidx.core.ktx) // WindowCompat
}
