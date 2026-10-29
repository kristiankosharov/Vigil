# Build logic: convention plugin code and analysis settings

Rules: `.claude/rules/build.md`. Once `build-logic`, `.editorconfig` and `config/detekt` exist, the
source files win. `libs` is the usual `VersionCatalogsExtension.named("libs")` helper.

## `vigil.hilt`: Android vs pure Kotlin modules

`ViewModelComponent` and the other Android components live in `hilt-android`, an Android library
that a pure Kotlin module can't depend on. Pure Kotlin modules get `hilt-core` and can install
modules only in `SingletonComponent`.

```kotlin
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        dependencies { "ksp"(libs.findLibrary("hilt-compiler").get()) }

        pluginManager.withPlugin("com.android.base") {          // Android modules
            pluginManager.apply("com.google.dagger.hilt.android")
            dependencies { "implementation"(libs.findLibrary("hilt-android").get()) }
        }
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {  // pure Kotlin modules
            dependencies { "implementation"(libs.findLibrary("hilt-core").get()) }
        }
    }
}
```

## Dependency rules check

Called from `vigil.android.application`, `vigil.android.library` and `vigil.jvm.library`.
Runs while Gradle configures the build, so a forbidden dependency fails every task immediately.

```kotlin
fun Project.enforceDependencyRules() {
    val from = path
    configurations.configureEach {
        val runtimeOnly = name.isRuntimeOnly()
        dependencies.withType<ProjectDependency>().configureEach {
            dependencyRuleViolation(from, to = path, runtimeOnly)?.let { reason ->
                throw GradleException("Forbidden dependency $from → $path: $reason (CLAUDE.md, Dependency rules)")
            }
        }
    }
}

/**
 * Why `from` may not depend on `to`, or null when the dependency is allowed. [runtimeOnly]: declared
 * in a `runtimeOnly` configuration, so `from` can't compile against `to`.
 */
internal fun dependencyRuleViolation(from: String, to: String, runtimeOnly: Boolean = false): String? {
    if (to == ":common:testing") return null // test helpers, any module
    if (to == from) return null // AGP adds a module's own project (e.g. :app for its test APK)
    val fromFeature = from.featureName() ?: return when { // :app, :common:*, :konsist
        to == ":app" -> "nothing depends on :app"

        from == ":app" && to.featureName() != null && !to.endsWith(":ui") ->
            ":app depends only on a feature's ui (which brings its data:core for Hilt)"

        from.startsWith(":common:") && to.featureName() != null -> ":common:* never depends on features"

        else -> null
    }
    val layer = from.substringAfter(":feature:$fromFeature:") // ui, domain, data:api, data:core
    val allowedCommon = when (layer) {
        "ui" -> setOf(":common:ui", ":common:core")
        "domain", "data:api" -> setOf(":common:core")
        else -> setOf(":common:core", ":common:network", ":common:datastore", ":common:security")
    }
    val allowedOwn = when (layer) {
        "ui" -> setOf("domain")
        "domain", "data:core" -> setOf("data:api")
        else -> emptySet()
    }
    val toFeature = to.featureName()
    // Only so Hilt in :app aggregates the bindings (FeatureUiConventionPlugin); ui code can't see it.
    if (layer == "ui" && to == ":feature:$fromFeature:data:core" && runtimeOnly) return null
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

/** `runtimeOnly`, or a variant's (`debugRuntimeOnly`, …). */
private fun String.isRuntimeOnly(): Boolean = this == "runtimeOnly" || endsWith("RuntimeOnly")

private fun String.featureName(): String? =
    if (startsWith(":feature:")) removePrefix(":feature:").substringBefore(":") else null
```

## `vigil.testing`: test logging

Failure messages must reach the console log (and `scripts/check.sh` reads the JUnit XML reports).

```kotlin
tasks.withType<Test>().configureEach {
    testLogging {
        events(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}
```

## `.editorconfig`

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space
indent_size = 4

[*.{kt,kts}]
# Android Kotlin style. ktlint's default (ktlint_official) forces extra wrapping, e.g. on every Kotest spec.
ktlint_code_style = android_studio
max_line_length = 120
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true
# Composables are PascalCase; without this ktlint rejects every @Composable function name.
ktlint_function_naming_ignore_when_annotated_with = Composable

[**/src/{test,androidTest}/**.kt]
# Kotest specs read `class XTest : FunSpec({`; class-signature would move the supertype to its own line.
ktlint_standard_class-signature = disabled

[*.{yml,yaml,json,toml}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

## `config/detekt/detekt.yml` (on top of the default config)

Rule names are from detekt 1.23; check them against the installed version.

```yaml
naming:
  FunctionNaming:
    ignoreAnnotated: ['Composable']
complexity:
  LongParameterList:
    ignoreAnnotated: ['Composable', 'Inject'] # Hilt constructors list one use case per action
  CyclomaticComplexMethod:
    ignoreSimpleWhenEntries: true # onAction's exhaustive `when` over every action
style:
  UnusedPrivateMember:
    ignoreAnnotated: ['Preview', 'PreviewLightDark', 'PreviewFontScale']
  MagicNumber:
    ignoreAnnotated: ['Preview', 'PreviewLightDark', 'PreviewFontScale']
potential-bugs:
  UnsafeCallOnNullableType:          # no !!
    active: true
coroutines:
  GlobalCoroutineUsage:              # no GlobalScope
    active: true
  SuspendFunSwallowedCancellation:   # no runCatching in suspend code
    active: true
  InjectDispatcher:                  # inject dispatchers, never hardcode Dispatchers.IO
    active: true
```
