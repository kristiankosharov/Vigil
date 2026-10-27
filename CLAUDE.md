# CLAUDE.md

Android app: multi-module, Clean Architecture, MVI. This file holds only the rules that apply to
**every** change. Follow it for every change; when a rule here conflicts with a habit or a generic
best practice, this file wins.

More detail loads only when needed:
- **`.claude/rules/`** — rules for one layer or area. Each file loads automatically when you work
  on files matching its `paths:`.
- **`.claude/skills/`** — step-by-step procedures (new feature, new screen,
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
| Database | **None right now.** `:common:database` (Room 3) was removed on 2026-10-26; every feature keeps its data in its own encrypted DataStore. To bring Room back, restore `:common:database`, the `vigil.room` plugin, `rules/room.md`, `reference/room.md` and the `add-room-entity` skill from git (commit `0c89b5e`). |
| Key-value / small data | Typed DataStore + `datastore-tink` (`AeadSerializer`) — one encrypted store per feature |
| UI | Jetpack Compose (BOM), Material 3, Navigation 3, Lifecycle ViewModel, `kotlinx-collections-immutable` |
| Paging | Paging 3 with Room + `RemoteMediator` (needs Room back first) |
| DI | Hilt (KSP) |
| Async | Kotlin Coroutines + Flow |
| Logging / crashes | Timber (Android modules only); Firebase Crashlytics in release, only in `:app` |
| Sign-in | Firebase Auth (email/password, phone, OpenID Connect via `OAuthProvider`), only in `:feature:auth:data:core` |
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
:common:datastore          → encrypted DataStore factory (Tink, AeadSerializer, JSON serializer), safeStoreCall
:common:security           → Vault (the PIN-unlocked data key, memory only), AesGcm, PIN key derivation
                             (Argon2id + Keystore pepper), biometric-bound Keystore key
:common:ui                 → theme, design system, UiText, BaseViewModel, effects, navigation handler
:common:testing            → Kotest extensions, main dispatcher, fixtures

:feature:<name>            → no code: what :app depends on (its ui + its data:core at runtime)
:feature:<name>:ui         → Screens, ViewModels, UiState / Action
:feature:<name>:domain     → domain models, use case interfaces + implementations
:feature:<name>:data:api   → PUBLIC: repository interfaces + `*Data` models
:feature:<name>:data:core  → INTERNAL: repository impls, Ktor API, DTOs, DataStore models, mappers, Hilt modules

:konsist                   → architecture & naming tests (JVM, Kotest + Konsist)
```

Add a `:common:*` module only for genuinely shared infrastructure, never for feature logic.

**Module graph.** `README.md` → "Module dependencies" has a Mermaid graph of the real module
dependencies. Any change that adds, removes or renames a module, or adds/removes a `projects.*`
dependency in a `build.gradle.kts` (including `:app`'s), updates that graph **in the same commit**,
read from the build files rather than from memory.

### Dependency rules (strict)

```
:app ───────────────► :feature:* (the feature module: api → its ui, runtimeOnly → its data:core),
                       :common:ui (tab icons), :common:network (EnvironmentModule's qualifiers)

:feature:x:ui ──────► :feature:x:domain, :common:ui, :common:core
:feature:x:domain ──► :feature:x:data:api, :common:core         (+ :feature:y:data:api if needed)
:feature:x:data:api ► :common:core
:feature:x:data:core ► :feature:x:data:api, :common:network, :common:datastore, :common:security,
                       :common:core                              (+ :feature:y:data:api if needed)
```

- `ui` **never** depends on any `data` module; it sees only domain models and use case interfaces.
- `domain` depends **only** on repository interfaces from `data:api` modules.
- Cross-feature dependencies go **only** to `:feature:y:data:api`. Shared infrastructure belongs in
  `:common:*`; IDs exchanged between features go through navigation keys.
- `:app` depends on each feature only through `:feature:x` (`vigil.feature`, no code), never on its
  `ui`, `domain` or `data` modules. `:feature:x` exposes `ui` and puts `data:core` on the runtime
  classpath only, so Hilt aggregates its bindings but `:app` can't compile against it.
- `:common:*` never depends on feature modules.
- Use `implementation`, not `api`, unless a type is part of the module's public signatures.
- These rules are enforced when Gradle configures the build (`enforceDependencyRules()` in
  `build-logic`). If it rejects a dependency, change the design, never the check.

### Visibility

- Everything in `data:core` is `internal` (implementations, DTOs, DataStore models, mappers, API
  classes, Hilt modules).
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
  `safeStoreCall`. Never swallow exceptions elsewhere; always rethrow
  `CancellationException`; never use `runCatching` in suspend code.
- **Never:** `!!`, `GlobalScope`, Android framework types in `domain` or `data:api`, `StateFlow` /
  `Channel` / `SharedFlow` for UI state or events, lambdas inside actions/effects/navigation,
  `SharedPreferences` or Preferences DataStore, reading `BuildConfig` outside `:app`.
- **Logging:** Timber only, in Android modules only, with an explicit tag. Log each error once,
  where it is mapped (the `safe*Call` wrappers). Never log tokens, passwords, personal data, request
  bodies or `Authorization` headers. No `android.util.Log` calls, `println`, `printStackTrace`.
- **Strings:** every user-facing string is a resource (see `.claude/rules/ui-quality.md`).

---

## Git workflow (git flow)

| Branch | From | Merges into | For |
|---|---|---|---|
| `main` | — | — | Released code only. Every merge is tagged `vX.Y.Z` (that tag runs the release workflow). |
| `develop` | `main` | — | Integration branch; the base of all regular work. |
| `feature/<short-name>` | `develop` | `develop` (PR) | New functionality. |
| `bugfix/<short-name>` | `develop` | `develop` (PR) | Fixes for bugs not yet released. |
| `release/<X.Y.Z>` | `develop` | `main` (PR), then back into `develop` | Stabilising a release: version-related changes and fixes only, no new features. |
| `hotfix/<X.Y.Z>` | `main` | `main` (PR), then back into `develop` | Urgent fixes to released code. |

- Never commit straight to `main` or `develop`; start the right branch type first. Names are
  lowercase with hyphens (`feature/pin-attempt-limit`, `bugfix/home-empty-state`).
- After a `release/*` or `hotfix/*` PR merges into `main`: tag `main` with `vX.Y.Z`, push the tag,
  and merge `main` back into `develop`.
- CI runs on every PR into `main` or `develop` and on pushes to `main`, `develop`, `release/*` and
  `hotfix/*`; a PR merges only with green CI.

- **Never run raw `./gradlew` builds or tests** — use `scripts/check.sh`, which prints one line
  per step plus, on failure, the failing tests with their messages and the compiler/detekt/lint
  findings (full logs in `build/check-logs/`):
  - `scripts/check.sh tests :feature:users:ui` — unit tests of the modules you changed
  - `scripts/check.sh android :feature:users:ui` — instrumented tests (Compose UI,
    Keystore); needs a running emulator, otherwise it says the tests did **not** run
  - `scripts/check.sh build` — compile the dev debug app
  - `scripts/check.sh detekt`, `scripts/check.sh release`, `scripts/check.sh all` (CI order)
- **Automatic (hooks in `.claude/settings.json`):** every Kotlin file you edit is formatted with the
  pinned ktlint, and problems it can't fix are reported back to you. If the hook says ktlint is
  missing or the wrong version, tell the user (formatting is then checked only by
  `scripts/check.sh format` / CI). When you finish a turn after changing Kotlin files, Konsist runs
  and blocks the stop with the violations — fix the code, not the test. When you add a new
  convention, add a Konsist rule for it (`.claude/reference/konsist-rules.md`).
- Before finishing: run the unit tests of the modules you changed, and the instrumented tests if
  you changed Compose UI tests or Keystore code.
- Full CI order and release checks: `.claude/skills/release-check/`.

---

## Index

| Area | Rule (auto-loaded by path) | Skill / reference |
|---|---|---|
| Results, errors, wrappers, repositories | `rules/data-layer.md` | `reference/core-result.md`, `reference/data-feature-example.md` |
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
| Design system, screen flows | `rules/ui-quality.md` | `docs/design/README.md`; components in `:common:ui` `designsystem/` |
