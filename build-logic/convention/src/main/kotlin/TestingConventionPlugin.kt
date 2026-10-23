import app.vigil.buildlogic.library
import app.vigil.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.withType

/** Kotest, MockK, Turbine and :common:testing for unit tests. Applied by every module plugin. */
class TestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        dependencies {
            "testImplementation"(libs.library("kotest-runner-junit5"))
            "testImplementation"(libs.library("kotest-assertions-core"))
            "testImplementation"(libs.library("mockk"))
            "testImplementation"(libs.library("turbine"))
            "testImplementation"(libs.library("kotlinx-coroutines-test"))
            "testRuntimeOnly"(libs.library("junit-platform-launcher"))
            if (path != ":common:testing" && rootProject.findProject(":common:testing") != null) {
                "testImplementation"(project(":common:testing"))
            }
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            // Failure messages must reach the log; scripts/check.sh also reads the JUnit XML reports.
            testLogging {
                events(TestLogEvent.FAILED)
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }
}
