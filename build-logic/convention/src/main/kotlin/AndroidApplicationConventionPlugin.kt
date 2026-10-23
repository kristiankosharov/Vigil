import app.vigil.buildlogic.SdkLevels
import app.vigil.buildlogic.configureAndroid
import app.vigil.buildlogic.enforceDependencyRules
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("vigil.testing")
        pluginManager.apply("vigil.detekt")

        extensions.configure<ApplicationExtension> {
            configureAndroid(this)
            defaultConfig.targetSdk = SdkLevels.TARGET
            buildTypes.getByName("release") {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
            lint {
                checkDependencies = true
                warningsAsErrors = true
                // Version bumps are a deliberate change of their own (CLAUDE.md, Versions), so a newer
                // release of a dependency must not fail the release build.
                disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
            }
        }
        enforceDependencyRules()
    }
}
