import app.vigil.buildlogic.enforceDependencyRules
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Pure Kotlin modules: data:api, domain, :common:core, :common:testing, :konsist. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("vigil.testing")
        pluginManager.apply("vigil.detekt")

        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(17)
            // Public contracts of a feature: every public declaration states its visibility and type.
            if (path.endsWith(":data:api") || path.endsWith(":domain")) explicitApi()
        }
        enforceDependencyRules()
    }
}
