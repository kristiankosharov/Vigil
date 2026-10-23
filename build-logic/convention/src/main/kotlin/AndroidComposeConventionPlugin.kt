import app.vigil.buildlogic.library
import app.vigil.buildlogic.libs
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/** Compose compiler + BOM. Compose versions come only from the BOM. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        extensions.getByType<CommonExtension>().buildFeatures.compose = true

        dependencies {
            val bom = platform(libs.library("androidx-compose-bom"))
            "implementation"(bom)
            "androidTestImplementation"(bom)
            "implementation"(libs.library("androidx-compose-ui"))
            "implementation"(libs.library("androidx-compose-ui-tooling-preview"))
            "implementation"(libs.library("androidx-compose-material3"))
            "debugImplementation"(libs.library("androidx-compose-ui-tooling"))
            "debugImplementation"(libs.library("androidx-compose-ui-test-manifest"))
            "androidTestImplementation"(libs.library("androidx-compose-ui-test-junit4"))
            "androidTestImplementation"(libs.library("androidx-compose-ui-test-junit4-accessibility"))
            "androidTestImplementation"(libs.library("androidx-test-runner"))
            "androidTestImplementation"(libs.library("androidx-test-ext-junit"))
        }
    }
}
