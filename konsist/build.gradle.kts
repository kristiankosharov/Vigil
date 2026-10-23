plugins {
    alias(libs.plugins.vigil.jvm.library)
}

dependencies {
    testImplementation(libs.konsist)
}

tasks.test {
    // Konsist reads the source files, not the compiled code, so the result depends on every module's
    // sources: never take it from the build cache.
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
}
