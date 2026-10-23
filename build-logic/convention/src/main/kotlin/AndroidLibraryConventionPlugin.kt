import app.vigil.buildlogic.configureAndroid
import app.vigil.buildlogic.enforceDependencyRules
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("vigil.testing")
        pluginManager.apply("vigil.detekt")

        extensions.configure<LibraryExtension> {
            configureAndroid(this)
            // Keep rules live next to the code they protect (rules/build.md, R8).
            if (file("consumer-rules.pro").exists()) defaultConfig.consumerProguardFiles("consumer-rules.pro")
        }
        enforceDependencyRules()
    }
}
