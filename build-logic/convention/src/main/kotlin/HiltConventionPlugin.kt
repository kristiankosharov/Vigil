import app.vigil.buildlogic.library
import app.vigil.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Hilt + KSP. ViewModelComponent and the other Android components live in hilt-android, an Android
 * library a pure Kotlin module can't depend on, so pure Kotlin modules get only hilt-core and install
 * their modules in SingletonComponent.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        listOf("com.android.application", "com.android.library").forEach { androidPlugin ->
            pluginManager.withPlugin(androidPlugin) {
                pluginManager.apply("com.google.devtools.ksp")
                pluginManager.apply("com.google.dagger.hilt.android")
                dependencies {
                    "implementation"(libs.library("hilt-android"))
                    "ksp"(libs.library("hilt-compiler"))
                }
            }
        }
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            pluginManager.apply("com.google.devtools.ksp")
            dependencies {
                "implementation"(libs.library("hilt-core"))
                "ksp"(libs.library("hilt-compiler"))
            }
        }
    }
}
