plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vigil.common.network"
}

dependencies {
    api(projects.common.core) // AppResult / AppError are part of this module's signatures
    api(libs.ktor.client.core) // feature APIs inject HttpClient
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
}
