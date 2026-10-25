# Vigil

An Android app built on a strict, tool-enforced architecture: multi-module Clean Architecture with
MVI, where every convention is checked by the build rather than by memory. It is also set up to be
developed together with [Claude Code](https://claude.com/claude-code).

<!-- TODO: one or two sentences on what Vigil does for its users. -->

## The idea

Most app codebases start clean and drift: layers leak into each other, error handling gets
inconsistent, and "we always do it this way" lives only in people's heads. Vigil is built the other
way round: the rules come first, and they are executable.

- **Every rule is enforced.** Module dependencies are checked when Gradle configures the build;
  architecture and naming rules run as [Konsist](https://docs.konsist.lemonappdev.com/) tests;
  detekt bans `!!`, `GlobalScope`, `runCatching` in suspend code and hardcoded dispatchers; ktlint
  formats every file; Android Lint and an R8 release build run in CI.
- **One way to do each thing.** Every repository returns `AppResult` and never throws; all I/O goes
  through `safeApiCall` / `safeDbCall` / `safeStoreCall`; every screen is the same MVI shape
  (`UiState`, `Action`, a ViewModel that builds its state with a `@Composable uiState()`, a
  logic-free `Route` and a stateless `Screen`).
- **Secure by default.** Small data lives in typed DataStores encrypted with Tink and an Android
  Keystore key; the whole app sits under a PIN + biometric lock; nothing sensitive is logged or
  written to saved state.
- **Ready for an AI pair programmer.** `CLAUDE.md`, `.claude/rules/`, `.claude/skills/` and
  `.claude/reference/` give Claude Code the same rules a human would follow, loaded only when the
  files being edited need them, and hooks format and check its changes automatically.

### What is there today

The features are wired through every layer but intentionally thin: the backend and real forms come
next.

| Feature | Status |
|---|---|
| **Login** (`:feature:auth`) | Placeholder screen; login always succeeds until the backend exists. Bearer-token plumbing (encrypted token store, refresh, session expiry → back to login) is real. |
| **App lock** (`:feature:applock`) | Working. PIN with a custom number pad (create + repeat), biometric unlock, locks on start and after 30 s in the background, covers the whole app. |
| **Home** (`:feature:home`) | Placeholder screen reading entries from Room (empty for now). |

Open items are marked in the code as `TODO(auth)`, `TODO(home)`, `TODO(applock)` and `TODO(env)`.

## Requirements

| Tool | Version | Notes |
|---|---|---|
| macOS or Linux | — | `scripts/check.sh` and the hooks are bash scripts. |
| JDK | 17 or newer | Compiles with a Java 17 toolchain. The Gradle daemon runs on **Java 21** (`gradle/gradle-daemon-jvm.properties`) and Gradle downloads it if missing; keep it at 21, because detekt 1.23 can't run on Java 25. |
| Android Studio | one that supports AGP 9.4 | Optional; everything also runs from the command line. |
| Android SDK | platform **37** | `compileSdk` / `targetSdk` 37, `minSdk` 26. |
| Device or emulator | Android 8.0 (API 26)+ | For running the app and the instrumented tests. |
| [ktlint](https://github.com/ktlint/ktlint/releases) | **1.8.0** exactly | The only formatter. The version is pinned in `gradle/libs.versions.toml`; a different version is reported as an error. Homebrew may ship an older one, so install it from the GitHub release if needed. |
| `jq` | any | Used by the Claude Code hooks (built into macOS 15+). |
| [Claude Code](https://claude.com/claude-code) | — | Optional. |

Gradle 9.8.2 comes with the wrapper; you don't need to install it.

## Getting started

```bash
git clone git@github.com:kristiankosharov/Vigil.git
```

Create `local.properties` with your SDK path (Android Studio does this when you open the project):

```properties
sdk.dir=/Users/you/Library/Android/sdk
```

Build the dev app, then install it on a running emulator or device:

```bash
scripts/check.sh build
```

```bash
adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

### Flavors and environments

| Flavor | Application ID | API base URL |
|---|---|---|
| `dev` | `app.vigil.dev` | placeholder (`TODO(env)`) |
| `staging` | `app.vigil.staging` | placeholder |
| `prod` | `app.vigil` | placeholder |

The URLs are `buildConfigField`s in `app/build.gradle.kts`; only `:app` reads `BuildConfig`.

Release builds are signed only when these environment variables are set (on CI); without them the
release build is unsigned but still runs R8:
`RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

## Checks

All checks go through one script that prints a line per step and, on failure, only what is needed
to fix it (failing tests with their messages, compiler/detekt/lint findings). Full logs are in
`build/check-logs/`.

| Command | What it does |
|---|---|
| `scripts/check.sh format` | ktlint on every Kotlin file (`ktlint --format <file>` fixes one) |
| `scripts/check.sh detekt` | static analysis with the Compose rules |
| `scripts/check.sh konsist` | architecture and naming rules |
| `scripts/check.sh tests [:module …]` | unit tests (Kotest, MockK, Turbine, Molecule) |
| `scripts/check.sh android [:module …]` | instrumented tests (Compose UI, accessibility checks); says clearly when no device is connected |
| `scripts/check.sh build` | compiles the dev debug app |
| `scripts/check.sh release` | Android Lint + R8 release build |
| `scripts/check.sh all` | the CI order: format, detekt, konsist, tests, lint, release |

## Architecture

```
:app                        Application, MainActivity, all navigation (Navigation 3), flavors

:common:core                AppResult, AppError, dispatchers, application scope (pure Kotlin)
:common:network             Ktor clients, bearer auth, safeApiCall
:common:database            the single Room 3 AppDatabase, one package per feature, safeDbCall
:common:datastore           encrypted typed DataStore (Tink), safeStoreCall
:common:ui                  theme, UiText, BaseViewModel, effect and navigation handlers
:common:testing             test helpers

:feature:<name>:ui          screens and ViewModels
:feature:<name>:domain      domain models and use cases (pure Kotlin)
:feature:<name>:data:api    repository interfaces (pure Kotlin)
:feature:<name>:data:core   repository implementations, APIs, storage (internal)

:konsist                    architecture tests
build-logic                 vigil.* convention plugins and the dependency-rule check
```

- `ui → domain → data:api`; `ui` never sees a `data` module, and features see each other only
  through `:data:api`.
- Everything crossing a boundary is an interface; implementations are `internal` and bound with
  Hilt.
- The app is layered as `AppLockHost { AuthSessionHost { NavDisplay } }`, so the lock screen covers
  everything.

The full rules are in [`CLAUDE.md`](CLAUDE.md) and `.claude/rules/`.

### Tech stack

| Area | Libraries |
|---|---|
| Build | AGP 9.4 (built-in Kotlin), Kotlin 2.4, KSP, Gradle 9.8, version catalog, convention plugins |
| UI | Jetpack Compose (BOM 2026.09), Material 3, Navigation 3, Lifecycle ViewModel |
| DI | Hilt (KSP), including pure-Kotlin modules |
| Data | Room 3 with the bundled SQLite driver, DataStore 1.3 + `datastore-tink`, Paging 3 |
| Network | Ktor 3 (OkHttp engine), kotlinx.serialization, bearer auth |
| Security | Tink, Android Keystore, `androidx.biometric` |
| Async / logging | Coroutines + Flow, Timber |
| Testing | Kotest, MockK, Turbine, Molecule, Compose UI tests, Ktor `MockEngine` |
| Quality | Konsist, detekt + Compose rules, ktlint, Android Lint |

Exact versions are in [`gradle/libs.versions.toml`](gradle/libs.versions.toml); the rule is always
the latest stable release.

## Working with Claude Code

- [`CLAUDE.md`](CLAUDE.md) holds the rules for every change; `.claude/rules/` adds rules per layer,
  loaded automatically when matching files are edited.
- `.claude/skills/` has step-by-step procedures: new feature, new screen, Room table, DataStore,
  paged list, release check.
- `.claude/reference/` has the reference code; once the real code exists, the source wins.
- Hooks in `.claude/settings.json` format every Kotlin file Claude edits with the pinned ktlint and
  run Konsist before it finishes a turn.

## License

[Apache License 2.0](LICENSE)
