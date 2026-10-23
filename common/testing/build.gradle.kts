plugins {
    alias(libs.plugins.vigil.jvm.library)
}

dependencies {
    api(libs.kotest.framework.engine)
    api(libs.kotlinx.coroutines.test)
}
