---
paths:
  # Not "app/**": the package is app.vigil, so that glob would match every source file.
  - "app/src/**"
  - "app/build.gradle.kts"
  - "app/*.pro"
---

# `:app`: navigation, environment, logging setup

Code: `.claude/reference/app.md`.

- `:app` contains `Application` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`), the theme
  host, **all navigation**, the flavors and the only `BuildConfig` in the project.
- `:app` depends only on `:feature:*:ui`, `:feature:*:data:core` (only for Hilt aggregation; never
  reference it in code), `:common:ui` (`AppTheme`) and `:common:network` (the qualifiers
  `EnvironmentModule` provides).

## Navigation 3

- The app owns the back stack and defines one `@Serializable` `NavKey` per destination. Feature
  modules never define or reference navigation keys.
- Each `entry<Key>` calls the feature's `XRoute` and connects its callbacks to back stack operations.
  Arguments travel in the key (IDs/primitives only, never whole models).
- Use `rememberSaveableStateHolderNavEntryDecorator()` and `rememberViewModelStoreNavEntryDecorator()`
  (from `androidx.lifecycle:lifecycle-viewmodel-navigation3`) so each destination gets its own
  ViewModel.
- If the Navigation 3 version requires registering key serializers (`SavedStateConfiguration` /
  polymorphic `serializersModule`), register every key and add new keys in the same change.
- `AuthSessionHost` from `:feature:auth:ui` wraps `NavDisplay`; on session expiry, clear the back
  stack and open the login key.
- **App lock (PIN + biometrics):** `AppLockHost` from `:feature:applock:ui` is the **outermost**
  layer: `AppLockHost { AuthSessionHost { NavDisplay } }`, so the lock screen covers everything.
  After login the app opens the create-PIN key, then `MainKey`. Code and rules:
  `.claude/reference/app-lock.md`.
- `MainActivity` is a `FragmentActivity` (BiometricPrompt needs one) and sets `FLAG_SECURE` on its
  window: no screenshots, screen recording or recents thumbnail anywhere in the app.

## Main tabs

- `MainKey` shows `MainTabs` (`navigation/MainTabs.kt`): a Material 3 `NavigationBar` and one tab
  per feature `XRoute` (Documents, Account; Reminders comes with its feature). The selected tab is
  `rememberSaveable`, each tab's state is kept in a `SaveableStateHolder`, and Back on another tab
  returns to the first. Tabs apply only the top/horizontal insets; the bar handles the bottom.
- Destinations opened from a tab (`AppearanceKey`, …) are pushed on the app back stack and cover
  the whole screen, bar included.

## Environment values

- `BASE_URL` and the debug flag are `buildConfigField`s here; `EnvironmentModule` (Hilt, `di`
  package) provides them as `@BaseUrl` and `@IsDebugBuild` (qualifiers declared in
  `:common:network`). No other module reads `BuildConfig`.
- Debug-only behaviour is gated on `BuildConfig.DEBUG` here, on `@IsDebugBuild` elsewhere, or lives
  in `src/debug`.
- Backup rules (`data_extraction_rules.xml`, `full_backup_content.xml`) exclude the `datastore/`
  directory and `datastore_keyset_prefs.xml`.

## Logging setup (Timber)

- Trees are planted **once** in `Application.onCreate`: `Timber.DebugTree()` in debug only; in
  release only a `ReleaseTree` that drops everything below `Log.WARN` and sends the rest to
  **Firebase Crashlytics**: every message as a breadcrumb, errors with a throwable as non-fatals.

- Always tag explicitly (`Timber.tag(TAG)`, `private const val TAG = "…"`): R8 renames classes, so
  automatic tags are unreadable in release.
- Pass the throwable first (`Timber.e(e, "…")`), never `e.message` in the text; use format arguments
  (`Timber.d("Loaded %d users", count)`), not string templates.
- Timber is used only in Android modules; `domain`, `data:api` and `:common:core` don't log.

## Firebase (Crashlytics)

- Project `vigil-app-25b0`; `app/google-services.json` holds all three flavors' apps
  (`app.vigil`, `app.vigil.staging`, `app.vigil.dev`). It's committed: it identifies the app, it
  isn't a secret. A new flavor or application ID needs a new app in that project and a fresh file.
- Firebase is used only in `:app` (Google Services + Crashlytics plugins, Firebase BOM), with one
  exception: **Firebase Auth** lives in `:feature:auth:data:core` (see `rules/network-auth.md`).
  Other features never call Firebase; they log with Timber and `ReleaseTree` forwards.
- Crash collection is off in debug builds and on in release (`manifestPlaceholders`
  `crashlyticsEnabled`); the R8 mapping is uploaded to Crashlytics only from CI (`CI` env var).
