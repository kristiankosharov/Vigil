# CLAUDE.md

Android app: multi-module, Clean Architecture, MVI. This file holds only the rules that apply to
**every** change. Follow it for every change; when a rule here conflicts with a habit or a generic
best practice, this file wins.

More detail loads only when needed:
- **`.claude/rules/`** — rules for one layer or area. Each file loads automatically when you work
  on files matching its `paths:`.
- **`.claude/skills/`** — step-by-step procedures (new feature, new screen, Room entity,
  DataStore, paged list, release check). Use them for those tasks.
- **`.claude/reference/`** — full code samples. Rules point to them; read one only when you need
  that code. Once the shared code exists in the project (`BaseViewModel`, wrappers,
  `encryptedDataStore`, Konsist tests, …), **the real source file wins** over the reference sample.

---

## Tech stack

| Area | Library |
|---|---|
| Language / build | Kotlin (AGP built-in Kotlin, K2), Gradle Kotlin DSL, version catalog, convention plugins in `build-logic` |
| Network | Ktor Client (OkHttp engine) + kotlinx.serialization, Ktor `Auth` (bearer) |
| Database | Room 3 (`androidx.room3`), `BundledSQLiteDriver`, KSP — one `AppDatabase` in `:common:database` |
| Key-value / small data | Typed DataStore + `datastore-tink` (`AeadSerializer`) — one encrypted store per feature |
| UI | Jetpack Compose (BOM), Material 3, Navigation 3, Lifecycle ViewModel, `kotlinx-collections-immutable` |
| Paging | Paging 3 with Room + `RemoteMediator` |
| DI | Hilt (KSP) |
| Async | Kotlin Coroutines + Flow |
| Logging | Timber (Android modules only) |
| Testing | Kotest, MockK, Turbine, Molecule (ViewModel state), Compose UI Test, Ktor `MockEngine` |
| Quality | Konsist (architecture tests), detekt + Compose rules, ktlint CLI (pinned version, the only formatter), Android Lint |

### Versions

- Always use the **latest stable** version. No alpha/beta/RC unless no stable release has the API
  we need, with a comment explaining why.
- Check the official release page before adding or bumping a dependency; never guess versions from
  memory. All versions live only in `gradle/libs.versions.toml`.
- The versions current when these rules were written are in `.claude/reference/versions.md`.
- AGP 9 has built-in Kotlin: do **not** apply `org.jetbrains.kotlin.android`. Use **KSP**, never
  kapt. Compose versions come only from the Compose BOM.
- **Verified in this project:** Room 3, Navigation 3, Konsist, DataStore 1.3 + `datastore-tink`,
  Hilt in pure Kotlin modules and the Compose accessibility checks now compile and run here; the
  real source files are the reference. **Still unverified:** Ktor `Auth` token clearing on logout
  and Paging 3 with Room 3. The first time you implement one, check it against the official docs
  and update the rule or reference if it differs.

---

## Architecture

- **MVI** in the presentation layer; **Clean Architecture** per feature: `ui` → `domain` → `data`.
- **Multi-module by feature.** A feature may only see another feature's `:data:api`, never its
  `ui`, `domain` or `data:core`.
- **Interfaces at every boundary.** Anything used by another layer or module is an interface;
  implementations are `internal`, named `<Interface>Impl`, and bound with Hilt.

### Modules

```
:app                       → Application, MainActivity, NavDisplay, all navigation, flavors, BuildConfig

:common:core               → AppResult, AppError, dispatchers, @ApplicationScope, UserDataCleaner, pure Kotlin utils
:common:network            → Ktor HttpClients, auth plugin, safeApiCall, @BaseUrl / @IsDebugBuild qualifiers
:common:database           → the single AppDatabase: all entities + DAOs (one package per feature), migrations, safeDbCall
:common:datastore          → encrypted DataStore factory (Tink, AeadSerializer, JSON serializer), safeStoreCall
:common:ui                 → theme, design system, UiText, BaseViewModel, effects, navigation handler
:common:testing            → Kotest extensions, main dispatcher, fixtures

:feature:<name>:ui         → Screens, ViewModels, UiState / Action
:feature:<name>:domain     → domain models, use case interfaces + implementations
:feature:<name>:data:api   → PUBLIC: repository interfaces + `*Data` models
:feature:<name>:data:core  → INTERNAL: repository impls, Ktor API, DTOs, DataStore models, mappers, Hilt modules

:konsist                   → architecture & naming tests (JVM, Kotest + Konsist)
```

Add a `:common:*` module only for genuinely shared infrastructure, never for feature logic.

### Dependency rules (strict)

```
:app ───────────────► :feature:*:ui, :feature:*:data:core,
                       :common:ui (AppTheme), :common:network (EnvironmentModule's qualifiers)

:feature:x:ui ──────► :feature:x:domain, :common:ui, :common:core
:feature:x:domain ──► :feature:x:data:api, :common:core         (+ :feature:y:data:api if needed)
:feature:x:data:api ► :common:core
:feature:x:data:core ► :feature:x:data:api, :common:network, :common:database, :common:datastore,
                       :common:core                              (+ :feature:y:data:api if needed)
```

- `ui` **never** depends on any `data` module; it sees only domain models and use case interfaces.
- `domain` depends **only** on repository interfaces from `data:api` modules.
- Cross-feature dependencies go **only** to `:feature:y:data:api`. Shared infrastructure belongs in
  `:common:*`; IDs exchanged between features go through navigation keys.
- `:app` depends on `data:core` only so Hilt can aggregate bindings; app code never references it.
- `:common:*` never depends on feature modules.
- Use `implementation`, not `api`, unless a type is part of the module's public signatures.
- These rules are enforced when Gradle configures the build (`enforceDependencyRules()` in
  `build-logic`). If it rejects a dependency, change the design, never the check.

### Visibility

- Everything in `data:core` is `internal` (implementations, DTOs, DataStore models, mappers, API
  classes, Hilt modules).
- In `:common:database`, entities and DAOs are `public` but each package belongs to **one** feature:
  `:feature:x:data:core` imports only `app.vigil.common.database.x.*` plus root helpers
  (`safeDbCall`, `asDbResult`). Nothing outside `data:core` modules and `:common:database` imports
  entities or DAOs; `AppDatabase` is used only inside `:common:database`.
- In `domain`, use case interfaces and domain models are public; `*UseCaseImpl`, mappers and Hilt
  modules are `internal`.
- In `ui`, only `<Feature>Route` composables and app-wide wrappers named `*Host` (`AuthSessionHost`,
  `AppLockHost`) are public.
- `explicitApi()` is on in `data:api` and `domain`.

### Hilt modules

- Every `@Module` lives in a `di` package, **one module per file, one concern per module**, named
  `<Feature><Concern>Module` (`UsersRepositoryModule`, `UsersDataStoreModule`, …).
- `@Binds` in an `interface`/`abstract class` module; `@Provides` in an `object` module; never both
  in one module.
- Pure Kotlin modules (`domain`, `data:api`, `:common:core`) see only `hilt-core`, so their modules
  are installed in `SingletonComponent`, unscoped. `ViewModelComponent` and other Android
  components exist only in Android modules.

---

## Conventions

- **Naming:** interfaces `XRepository`, `XUseCase`, `XApi`; implementations `XRepositoryImpl`,
  `XUseCaseImpl`, `XApiImpl` — **never** `DefaultX`, `XImplementation`, `RealX`. Also `XDao`,
  `XEntity`, `XDto`, `XData` (data:api models), domain models without suffix, `XItem` (UI models),
  `XViewModel`, `XUiState`, `XAction`, `XNavigation`, `XRoute`, `XScreen`.
- **Packages:** `app.vigil.feature.<name>.<layer>`; Hilt modules in `….<layer>.di`.
- **Errors:** repositories return `AppResult` and never throw. All I/O goes through `safeApiCall` /
  `safeDbCall` / `safeStoreCall`. Never swallow exceptions elsewhere; always rethrow
  `CancellationException`; never use `runCatching` in suspend code.
- **Never:** `!!`, `GlobalScope`, Android framework types in `domain` or `data:api`, `StateFlow` /
  `Channel` / `SharedFlow` for UI state or events, lambdas inside actions/effects/navigation,
  `SharedPreferences` or Preferences DataStore, reading `BuildConfig` outside `:app`.
- **Logging:** Timber only, in Android modules only, with an explicit tag. Log each error once,
  where it is mapped (the `safe*Call` wrappers). Never log tokens, passwords, personal data, request
  bodies or `Authorization` headers. No `android.util.Log` calls, `println`, `printStackTrace`.
- **Strings:** every user-facing string is a resource (see `.claude/rules/ui-quality.md`).

---

## Checks

- **Never run raw `./gradlew` builds or tests** — use `scripts/check.sh`, which prints one line
  per step plus, on failure, the failing tests with their messages and the compiler/detekt/lint
  findings (full logs in `build/check-logs/`):
  - `scripts/check.sh tests :feature:users:ui` — unit tests of the modules you changed
  - `scripts/check.sh android :feature:users:ui` — instrumented tests (Compose UI, Room
    migrations); needs a running emulator, otherwise it says the tests did **not** run
  - `scripts/check.sh build` — compile the dev debug app
  - `scripts/check.sh detekt`, `scripts/check.sh release`, `scripts/check.sh all` (CI order)
- **Automatic (hooks in `.claude/settings.json`):** every Kotlin file you edit is formatted with the
  pinned ktlint, and problems it can't fix are reported back to you. If the hook says ktlint is
  missing or the wrong version, tell the user (formatting is then checked only by
  `scripts/check.sh format` / CI). When you finish a turn after changing Kotlin files, Konsist runs
  and blocks the stop with the violations — fix the code, not the test. When you add a new
  convention, add a Konsist rule for it (`.claude/reference/konsist-rules.md`).
- Before finishing: run the unit tests of the modules you changed, and the instrumented tests if
  you changed Compose UI tests or a Room migration.
- Full CI order and release checks: `.claude/skills/release-check/`.

---

## Index

| Area | Rule (auto-loaded by path) | Skill / reference |
|---|---|---|
| Results, errors, wrappers, repositories | `rules/data-layer.md` | `reference/core-result.md`, `reference/data-feature-example.md` |
| Room | `rules/room.md` | `skills/add-room-entity`, `reference/room.md` |
| DataStore + encryption | `rules/datastore.md` | `skills/add-datastore`, `reference/datastore.md` |
| Network, auth, tokens | `rules/network-auth.md` | `reference/network-auth.md` |
| Use cases | `rules/domain.md` | `reference/data-feature-example.md` |
| MVI, ViewModel, effects, Compose | `rules/ui-mvi.md` | `skills/new-screen`, `reference/mvi-base.md`, `reference/screen-example.md` |
| Strings, accessibility | `rules/ui-quality.md` | — |
| App, navigation, logging setup | `rules/app.md` | `reference/app.md` |
| App lock (PIN, biometrics) | `rules/app.md` | `reference/app-lock.md` |
| Build, flavors, R8, detekt, ktlint | `rules/build.md` | `skills/release-check`, `reference/build-logic.md`, `reference/versions.md` |
| Paging | `rules/data-layer.md`, `rules/ui-mvi.md` | `skills/add-paging-list` |
| Tests, Konsist | `rules/testing.md` | `reference/tests.md`, `reference/konsist-rules.md` |
| New feature | — | `skills/new-feature` |
