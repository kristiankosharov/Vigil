---
paths:
  - "app/**"
---

# `:app`: navigation, environment, logging setup

Code: `.claude/reference/app.md`.

- `:app` contains `Application` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`), the theme
  host, **all navigation**, the flavors and the only `BuildConfig` in the project.
- `:app` depends only on `:feature:*:ui` and `:feature:*:data:core` (the latter only for Hilt
  aggregation; never reference it in code).

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
  release only a `ReleaseTree` that drops everything below `Log.WARN` and forwards warnings and errors
  to the crash reporter once one is added.
- Always tag explicitly (`Timber.tag(TAG)`, `private const val TAG = "…"`): R8 renames classes, so
  automatic tags are unreadable in release.
- Pass the throwable first (`Timber.e(e, "…")`), never `e.message` in the text; use format arguments
  (`Timber.d("Loaded %d users", count)`), not string templates.
- Timber is used only in Android modules; `domain`, `data:api` and `:common:core` don't log.
