import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** The dev/staging/prod flavors. Applied only to :app; library modules have no flavors. */
class EnvironmentConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        check(path == ":app") { "vigil.environment is only for :app (rules/build.md, Variants)" }
        extensions.configure<ApplicationExtension> {
            flavorDimensions += "environment"
            productFlavors {
                create("dev") {
                    dimension = "environment"
                    applicationIdSuffix = ".dev"
                }
                create("staging") {
                    dimension = "environment"
                    applicationIdSuffix = ".staging"
                }
                create("prod") {
                    dimension = "environment"
                }
            }
        }
    }
}
