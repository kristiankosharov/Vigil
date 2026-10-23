plugins {
    alias(libs.plugins.vigil.android.application)
    alias(libs.plugins.vigil.android.compose)
    alias(libs.plugins.vigil.hilt)
    alias(libs.plugins.vigil.environment)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vigil"

    defaultConfig {
        applicationId = "app.vigil"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    productFlavors {
        // TODO(env): the real API URLs.
        getByName("dev") { buildConfigField("String", "BASE_URL", "\"https://dev.api.vigil.app/\"") }
        getByName("staging") { buildConfigField("String", "BASE_URL", "\"https://staging.api.vigil.app/\"") }
        getByName("prod") { buildConfigField("String", "BASE_URL", "\"https://api.vigil.app/\"") }
    }

    // Release signing only when CI provides the keystore. Without it the release build is
    // unsigned but still runs R8, so scripts/check.sh release works locally.
    val keystorePath = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        release { signingConfig = signingConfigs.findByName("release") }
    }
}

dependencies {
    implementation(projects.feature.home.ui)
    implementation(projects.feature.home.data.core) // Hilt aggregation only; never referenced in code

    implementation(projects.common.ui) // AppTheme
    implementation(projects.common.network) // @BaseUrl / @IsDebugBuild, provided by EnvironmentModule

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}
