plugins {
    alias(libs.plugins.vigil.feature.ui) // adds :common:ui
}

android {
    namespace = "app.vigil.feature.applock.ui"
}

dependencies {
    implementation(projects.feature.applock.domain)
    implementation(projects.common.core)
    implementation(libs.androidx.biometric) // BiometricPrompt
    implementation(libs.androidx.fragment) // BiometricPrompt needs a FragmentActivity
    implementation(libs.androidx.lifecycle.process) // app background/foreground
    implementation(libs.androidx.activity.compose) // BackHandler, LocalActivity
}
