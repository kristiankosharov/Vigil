package app.vigil.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

internal object SdkLevels {
    const val COMPILE = 37
    const val TARGET = 37
    const val MIN = 26
}

internal val JAVA_VERSION = JavaVersion.VERSION_17

/** Defaults shared by the app and every Android library. AGP's built-in Kotlin takes its JVM target from these. */
internal fun configureAndroid(android: CommonExtension) {
    android.compileSdk = SdkLevels.COMPILE
    android.defaultConfig.minSdk = SdkLevels.MIN
    android.defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    android.compileOptions.sourceCompatibility = JAVA_VERSION
    android.compileOptions.targetCompatibility = JAVA_VERSION
    android.testOptions.unitTests.all { it.useJUnitPlatform() } // Kotest
    // android.jar is only stubs in unit tests; Compose (Molecule ViewModel tests) calls Trace and Log.
    android.testOptions.unitTests.isReturnDefaultValues = true
    android.packaging.resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/LICENSE*.md")
}
