import app.vigil.buildlogic.library
import app.vigil.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A feature's ui module: Android library + Compose + Hilt + ViewModel/Compose libraries + :common:ui.
 * Its own data:core goes on the runtime classpath only: ui code can't compile against it, but Hilt
 * in :app (which depends only on ui modules) still aggregates its bindings.
 */
class FeatureUiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("vigil.android.library")
        pluginManager.apply("vigil.android.compose")
        pluginManager.apply("vigil.hilt")

        dependencies {
            "implementation"(project(":common:ui"))
            "runtimeOnly"(project(path.removeSuffix(":ui") + ":data:core"))
            "implementation"(libs.library("androidx-lifecycle-viewmodel-compose"))
            "implementation"(libs.library("androidx-lifecycle-runtime-compose"))
            "implementation"(libs.library("hilt-lifecycle-viewmodel-compose"))
            "implementation"(libs.library("kotlinx-collections-immutable"))
            "testImplementation"(libs.library("molecule-runtime"))
        }
    }
}
