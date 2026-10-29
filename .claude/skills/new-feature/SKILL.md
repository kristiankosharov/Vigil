---
name: new-feature
description: Create a new feature module set (data:api, data:core, domain, ui) and wire it into the app. Use when the user asks to add, create or scaffold a new feature, module or section of the app.
---

# New feature `foo`

Follow these steps in order. Layer rules load automatically from `.claude/rules/` as you touch each
module; code to copy is in `.claude/reference/`.

1. **Modules.** Create `:feature:foo:data:api`, `:feature:foo:data:core`, `:feature:foo:domain`,
   `:feature:foo:ui` and include them in `settings.gradle.kts`. Build files use only convention
   plugins (templates below). The build fails on a forbidden dependency (`enforceDependencyRules()`).
2. **`data:api`.** `FooRepository` interface + `*Data` models (`explicitApi()`, no Ktor/
   serialization annotations).
3. **Local storage** (`.claude/rules/datastore.md`; there is no database right now):
   - DataStore → use the `add-datastore` skill (`FooPreferences`, `encryptedDataStore("foo_<name>.json", …)`,
     `UserDataCleaner` if it holds user data).
4. **`data:core`.** DTOs, `FooApi` + `FooApiImpl` (inject `@Authenticated HttpClient`), mappers,
   `FooRepositoryImpl` using `safeApiCall` / `safeStoreCall`, one Hilt module per
   concern in `di` (`FooApiModule`, `FooRepositoryModule`, …). Everything `internal`.
   Reference: `.claude/reference/data-feature-example.md`.
5. **`domain`.** Domain models + `internal` mappers from `*Data`; `*UseCase` interfaces — SAM
   `fun interface` provided as a lambda for one-liners (`FooUseCaseModule`), `*UseCaseImpl` + `@Binds`
   otherwise (`FooUseCaseBindsModule`), both in `SingletonComponent`, unscoped (pure Kotlin module:
   no `ViewModelComponent`).
6. **`ui`.** Use the `new-screen` skill for each screen. Use the `add-paging-list` skill if a list is
   unbounded.
7. **Strings and accessibility.** Strings in `:feature:foo:ui` resources prefixed `foo_`; content
   descriptions, 48dp touch targets, headings, `@PreviewLightDark` + `@PreviewFontScale`
   (`.claude/rules/ui-quality.md`).
8. **`:app`.** Add only `implementation(projects.feature.foo.ui)` (never its `domain` or `data` modules; the `ui` brings `data:core` for Hilt); add the `NavKey`(s) and `entry<…>` wiring
   (`.claude/reference/app.md`).
9. **Module graph.** Add the feature to the Mermaid graph in `README.md` ("Module dependencies"):
   a `subgraph` with `ui → domain → data:api ← data:core` and `ui -.-> data:core` (dotted, runtime only),
   `:app`'s edge to its `ui`, and its `data:core` edges to the `:common` modules it uses.
10. **Tests.** Kotest/MockK/Turbine for the repository, every `*UseCaseImpl` and every ViewModel;
   Compose UI tests (with accessibility checks) for each screen (`.claude/rules/testing.md`).
11. **Checks.** `scripts/check.sh tests :feature:foo:data:core :feature:foo:domain :feature:foo:ui`,
    `scripts/check.sh android :feature:foo:ui` (Compose UI tests; needs an emulator),
    `scripts/check.sh detekt`, and `scripts/check.sh release` (R8). Konsist runs automatically.

## Build file templates

Check the plugin aliases against `gradle/libs.versions.toml` and `build-logic` once they exist.

```kotlin
// feature/foo/data/api/build.gradle.kts
plugins { alias(libs.plugins.vigil.jvm.library) }
dependencies {
    implementation(projects.common.core)
}

// feature/foo/data/core/build.gradle.kts
plugins {
    alias(libs.plugins.vigil.android.library)
    alias(libs.plugins.vigil.hilt)
}
android { namespace = "app.vigil.feature.foo.data" }
dependencies {
    implementation(projects.feature.foo.data.api)
    implementation(projects.common.core)
    implementation(projects.common.network)
    implementation(projects.common.datastore)    // only if it uses DataStore
}

// feature/foo/domain/build.gradle.kts
plugins {
    alias(libs.plugins.vigil.jvm.library)
    alias(libs.plugins.vigil.hilt)             // adds only hilt-core + KSP in a pure Kotlin module
}
dependencies {
    implementation(projects.feature.foo.data.api)
    implementation(projects.common.core)
}

// feature/foo/ui/build.gradle.kts
plugins { alias(libs.plugins.vigil.feature.ui) }  // adds :common:ui, runtimeOnly(:data:core)
android { namespace = "app.vigil.feature.foo.ui" }
dependencies {
    implementation(projects.feature.foo.domain)
    implementation(projects.common.core)
}
```
