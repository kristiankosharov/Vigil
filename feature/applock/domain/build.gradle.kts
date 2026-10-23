plugins {
    alias(libs.plugins.vigil.jvm.library)
    alias(libs.plugins.vigil.hilt) // adds only hilt-core + KSP in a pure Kotlin module
}

dependencies {
    implementation(projects.feature.applock.data.api)
    implementation(projects.common.core)
}
