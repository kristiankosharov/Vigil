import app.vigil.buildlogic.library
import app.vigil.buildlogic.libs
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** detekt + Compose rules on every module. No detekt-formatting: the pinned ktlint CLI formats. */
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.gitlab.arturbosch.detekt")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            parallel = true
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        }
        dependencies {
            "detektPlugins"(libs.library("compose-rules-detekt"))
        }
        // detekt 1.23 runs on the Kotlin compiler it was built with; Gradle would otherwise upgrade
        // it to the project's Kotlin version and detekt fails to start.
        configurations.matching { it.name == "detekt" }.configureEach {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin") useVersion(DETEKT_KOTLIN_VERSION)
            }
        }
    }

    private companion object {
        const val DETEKT_KOTLIN_VERSION = "2.0.21"
    }
}
