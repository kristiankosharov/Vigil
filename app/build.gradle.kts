plugins {
    alias(libs.plugins.vigil.android.application)
    alias(libs.plugins.vigil.android.compose)
    alias(libs.plugins.vigil.hilt)
    alias(libs.plugins.vigil.environment)
    alias(libs.plugins.kotlin.serialization)
    // Firebase: app/google-services.json holds all three flavors' apps (project vigil-app-25b0).
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "app.vigil"

    defaultConfig {
        applicationId = "app.vigil"
        // The release workflow sets these from the git tag (v1.2.3 → 1.2.3 / 1002003); local builds are 1 / dev.
        versionCode = providers.environmentVariable("VERSION_CODE").map(String::toInt).orElse(1).get()
        versionName = providers.environmentVariable("VERSION_NAME").orElse("0.1.0-dev").get()
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
        debug {
            manifestPlaceholders["crashlyticsEnabled"] = false // no crash reports from development builds
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            manifestPlaceholders["crashlyticsEnabled"] = true
            // Upload the R8 mapping only from CI (release workflow), never from a local check.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = providers.environmentVariable("CI").isPresent
            }
        }
    }
}

dependencies {
    implementation(projects.feature.applock)
    implementation(projects.feature.auth)
    implementation(projects.feature.documents)
    implementation(projects.feature.settings)

    implementation(projects.common.ui) // tab icons (MainTabs)
    implementation(projects.common.network) // @BaseUrl / @IsDebugBuild, provided by EnvironmentModule

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment) // MainActivity is a FragmentActivity for BiometricPrompt
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
}
