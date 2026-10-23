---
paths:
  - "build-logic/**"
  - "gradle/**"
  - "**/build.gradle.kts"
  - "settings.gradle.kts"
  - "**/*.pro"
  - "config/detekt/**"
  - ".editorconfig"
  - ".github/**"
---

# Build setup, variants, R8, static analysis

Versions current when written: `.claude/reference/versions.md`. Convention plugin code (Hilt,
dependency rules, test logging, detekt/editorconfig settings): `.claude/reference/build-logic.md`.
Flavor/BuildConfig/signing code: `.claude/reference/app.md`. Release procedure:
`.claude/skills/release-check/`.

## Convention plugins (`build-logic/convention`)

- `vigil.android.application` — app module
- `vigil.android.library` — Android library defaults (SDK levels, JVM toolchain, lint,
  `testOptions.unitTests.all { it.useJUnitPlatform() }` for Kotest)
- `vigil.jvm.library` — pure Kotlin modules (`data:api`, `domain`, `:common:core`, `:konsist`)
- `vigil.android.compose` — Compose compiler + BOM
- `vigil.hilt` — Hilt + KSP compiler. Android modules get the Hilt Gradle plugin and `hilt-android`;
  pure Kotlin modules get **only `hilt-core`** (no Hilt Gradle plugin, no Android components such as
  `ViewModelComponent`).
- `vigil.room` — Room 3 + KSP + schema directory, **only** for `:common:database`
- `vigil.feature.ui` — library + compose + hilt + lifecycle/hilt-compose deps + `:common:ui`
- `vigil.testing` — Kotest, MockK, Turbine, `:common:testing`; test logging with
  `exceptionFormat = FULL`. Applied by the three module plugins
- `vigil.environment` — `dev`/`staging`/`prod` flavors, **only** for `:app`
- `vigil.detekt` — detekt + Compose rules; applied by the three module plugins, so every module has it

The three module plugins (`android.application`, `android.library`, `jvm.library`) call
`enforceDependencyRules()`, which fails the build on a project dependency that breaks the dependency
rules in `CLAUDE.md`. Never weaken it to make a dependency compile; fix the dependency.

Module build files are a few lines: plugins + module-specific dependencies. Use type-safe project
accessors (`projects.feature.users.data.api`). All versions live in `gradle/libs.versions.toml`.

## Variants

- Build types `debug` and `release`; release has `isMinifyEnabled = true`, `isShrinkResources = true`.
- One flavor dimension, `environment`: `dev`, `staging`, `prod`. `dev`/`staging` use
  `applicationIdSuffix` (`.dev`, `.staging`).
- Flavors exist **only in `:app`**. Library modules have no flavors, so no `missingDimensionStrategy`
  is needed.
- `buildFeatures.buildConfig = true` only in `:app` (AGP 9 turns it off by default).
- **Secrets** (API keys, signing passwords) are never committed. Read them with
  `providers.gradleProperty(...)` / `providers.environmentVariable(...)` — `local.properties` locally,
  CI secrets on CI.
- **Release signing** comes only from environment variables and is applied **only when they are
  set**. Without them the release build is unsigned but still runs R8, so `scripts/check.sh release`
  works locally.

## R8 and kotlinx.serialization

- R8 (full mode) runs on every release build; a change isn't done until `scripts/check.sh release`
  passes.
- kotlinx.serialization, Ktor, Room, Hilt and Navigation ship their own consumer rules; don't copy them.
- Keep rules next to the code they protect: each library module's `consumer-rules.pro`
  (`consumerProguardFiles`); `:app/proguard-rules.pro` only for app-level rules.
- Add a rule only when a `@Serializable` class is looked up by reflection (runtime `KType`,
  polymorphic `NavKey`s) and the release build fails or crashes; use the narrowest rule scoped to our
  package (sample in `.claude/skills/release-check/`).
- Never `-keep class app.vigil.** { *; }` or `-dontobfuscate`. Upload `mapping.txt` for every release.

## Formatting and static analysis

- **ktlint** (the CLI, version pinned as `ktlint` in `gradle/libs.versions.toml`) is the only
  formatter, with `ktlint_code_style = android_studio` (`.editorconfig`). A hook formats every
  Kotlin file you edit; `scripts/check.sh format` checks all files (first CI step). When you bump
  the pin, everyone (and CI) installs the same version.
- **detekt** with the Compose rules plugin (`io.nlopez.compose.rules:detekt`) does code analysis,
  applied by `vigil.detekt`. Do **not** add `detekt-formatting`: it bundles its own ktlint version,
  which would disagree with the pinned CLI. Run `scripts/check.sh detekt`.
- The Gradle daemon runs on Java 21 (`gradle/gradle-daemon-jvm.properties`): detekt 1.23 crashes
  on Java 25, so don't let the IDE raise it.
- `.editorconfig` and `config/detekt/detekt.yml` must keep the settings in
  `.claude/reference/build-logic.md`: Composable function names, preview functions, and the rules
  behind `CLAUDE.md`'s "Never" list (`!!`, `GlobalScope`, `runCatching` in suspend code,
  hardcoded dispatchers).
- No new baseline entries; no `@Suppress` without a comment saying why. Expected suppressions: the
  `safe*Call` wrappers and `errorBodyOrNull` (`TooGenericExceptionCaught`, `SwallowedException`) and
  the dispatcher provider in `:common:core` (`InjectDispatcher`).
- Android Lint on `:app` with `checkDependencies = true`, `warningsAsErrors = true` for release;
  `lint-baseline.xml` only for issues that predate the rule.
