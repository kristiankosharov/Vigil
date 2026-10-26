package app.vigil.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.withType

/**
 * Fails the build while Gradle configures it when a project dependency breaks the dependency rules
 * in CLAUDE.md. Called by the three module plugins (android.application, android.library, jvm.library).
 * Never weaken it to make a dependency compile; change the design.
 */
internal fun Project.enforceDependencyRules() {
    val from = path
    configurations.configureEach {
        dependencies.withType<ProjectDependency>().configureEach {
            dependencyRuleViolation(from, to = path)?.let { reason ->
                throw GradleException("Forbidden dependency $from → $path: $reason (CLAUDE.md, Dependency rules)")
            }
        }
    }
}

/** Why `from` may not depend on `to`, or null when the dependency is allowed. */
internal fun dependencyRuleViolation(from: String, to: String): String? {
    if (to == ":common:testing") return null // test helpers, any module
    if (to == from) return null // AGP adds a module's own project (e.g. :app for its test APK)
    val fromFeature = from.featureName() ?: return when { // :app, :common:*, :konsist
        to == ":app" -> "nothing depends on :app"

        from.startsWith(":common:") && to.featureName() != null -> ":common:* never depends on features"

        else -> null
    }
    val layer = from.substringAfter(":feature:$fromFeature:") // ui, domain, data:api, data:core
    val allowedCommon = when (layer) {
        "ui" -> setOf(":common:ui", ":common:core")
        "domain", "data:api" -> setOf(":common:core")
        else -> setOf(":common:core", ":common:network", ":common:database", ":common:datastore", ":common:security")
    }
    val allowedOwn = when (layer) {
        "ui" -> setOf("domain")
        "domain", "data:core" -> setOf("data:api")
        else -> emptySet()
    }
    val toFeature = to.featureName()
    return when (toFeature) {
        null -> "$layer may use only ${allowedCommon.joinToString()}".takeIf { to !in allowedCommon }

        fromFeature ->
            "$layer may use only its own ${allowedOwn.joinToString().ifEmpty { "(nothing)" }}"
                .takeIf { to.substringAfter(":feature:$toFeature:") !in allowedOwn }

        else ->
            "another feature is visible only through its :data:api, from domain or data:core"
                .takeUnless { layer in setOf("domain", "data:core") && to.endsWith(":data:api") }
    }
}

private fun String.featureName(): String? =
    if (startsWith(":feature:")) removePrefix(":feature:").substringBefore(":") else null
