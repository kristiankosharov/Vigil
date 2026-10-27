import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A feature's own module (`:feature:foo`), the only one `:app` depends on. It has no code: it exposes
 * the feature's ui (its `*Route`s and `*Host`s) and puts its data:core on the runtime classpath, where
 * Hilt aggregates the bindings, so `:app` can't compile against data:core.
 */
class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("vigil.android.library")

        extensions.configure<LibraryExtension> {
            namespace = "app.vigil.feature.$name"
        }
        dependencies {
            "api"(project("$path:ui"))
            "runtimeOnly"(project("$path:data:core"))
        }
    }
}
