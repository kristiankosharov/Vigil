import androidx.room3.gradle.RoomExtension
import app.vigil.buildlogic.library
import app.vigil.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Room 3 with KSP and exported schemas. Applied only to :common:database. */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        check(path == ":common:database") { "vigil.room is only for :common:database (rules/room.md)" }
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("androidx.room3")

        extensions.configure<RoomExtension> {
            schemaDirectory(layout.projectDirectory.dir("schemas"))
        }
        dependencies {
            "implementation"(libs.library("androidx-room3-runtime"))
            "implementation"(libs.library("androidx-sqlite-bundled"))
            "ksp"(libs.library("androidx-room3-compiler"))
            "androidTestImplementation"(libs.library("androidx-room3-testing"))
        }
    }
}
