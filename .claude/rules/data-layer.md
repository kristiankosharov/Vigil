---
paths:
  - "feature/*/data/**"
  - "common/core/**"
  - "common/network/**"
  - "common/database/**"
  - "common/datastore/**"
---

# Data layer: results, errors, wrappers, repositories

Code: `.claude/reference/core-result.md` (AppResult, AppError, wrappers) and
`.claude/reference/data-feature-example.md` (a full feature: data:api, data:core, Hilt modules).

## `AppResult` and `AppError` (`:common:core`)

- Every repository function that touches the network, Room or DataStore returns `AppResult<T>` or
  `Flow<AppResult<T>>`. Never throw out of a repository; never return a nullable to signal failure.
  Only calls on in-memory state that cannot fail (e.g. the app-lock session) return plain values.
- `AppResult` has `map`, `onSuccess`, `onFailure`, and `Flow<AppResult<T>>.mapResult`.
- `AppError` is structured by source:
  - `Network`: `NoConnection`, `Timeout`, `Http(code, body: ApiErrorBody?)`, `Serialization`, `Unknown`
  - `Local`: `NotFound`, `Constraint`, `Database`, `Storage` (DataStore I/O), `Unknown`
  - `Auth`: sign-in failures the UI tells apart (`rules/network-auth.md`); only `:feature:auth`
    raises them, through its `safeFirebaseCall`
  - top-level `Unknown`
- Add feature-specific error subtypes only if the UI must react to them differently.
- Paging errors carry an `AppError` inside `AppErrorException(val error: AppError) : Exception()`
  (`:common:core`).

## Wrappers

- All network, Room and DataStore access goes through `safeApiCall` (`:common:network`),
  `safeDbCall` / `asDbResult()` (`:common:database`) and `safeStoreCall` / `asStoreResult()`
  (`:common:datastore`).
- They catch `Exception` (never `Throwable`: `Error`s must crash), map it to an `AppError`, and
  **always rethrow `CancellationException`**. Never use `runCatching` in suspend code.
- They are public `inline` functions, so the functions they call (`toNetworkError`,
  `toLocalError`, `toStoreError`, `errorBodyOrNull`) must be public too.
- Mapping: timeouts → `Timeout`; `IOException` / unresolved address → `NoConnection`;
  `ResponseException` → `Http` with the parsed `ApiErrorBody`; serialization errors →
  `Serialization`; SQLite constraint → `Local.Constraint`; other `SQLiteException` →
  `Local.Database`; DataStore `IOException` → `Local.Storage`.
- The wrappers are the only place errors are logged (Timber, `w`; unexpected `Unknown` at `e`).
- Observed flows use `asRetryingResult` (`:common:core`) through `asDbResult` / `asStoreResult`:
  each failure is emitted and the source re-subscribed with a growing delay. The failure counter
  **resets after every successful emission**, so only `maxRetries` *consecutive* failures end the
  flow. The counter lives inside `flow { }`, so each collector has its own. When the flow ends, the
  ViewModel subscribes again on its `Refresh` action.

## `data:api` (public contract)

- Only `*Repository` interfaces and the `*Data` models in their signatures: plain immutable
  `data class`es with no Ktor, Room or serialization annotations.
- May depend on `androidx.paging:paging-common` when a repository returns `Flow<PagingData<XData>>`.

## `data:core` (internal implementation)

- Layout: `remote/` (`XApi` + `XApiImpl`, DTOs), `local/` (DataStore models), `mapper/`,
  `repository/`, `di/`. Entities and DAOs come from the feature's package in `:common:database`.
- Remote and local sources are behind interfaces (`XApi`, `XDao`, `DataStore<T>`) so repositories
  can be tested with mocks.
- **Offline-first:** Room is the single source of truth for lists. Observed data comes from Room;
  network results are written into Room, not returned directly.
- DTOs, entities and DataStore models never leave `data:core`; map to `*Data` at the repository
  boundary.
- Inject the qualified `CoroutineDispatcher` from `:common:core`; never hardcode `Dispatchers.IO`.
- Choose Room or DataStore with the table in `.claude/rules/datastore.md`. Never store the same
  data in both.

## Clearing user data on logout

`:common:core` defines `fun interface UserDataCleaner { suspend fun clear() }`. `:common:database`
contributes one that clears user tables; every feature that keeps user data in DataStore contributes
one that resets its stores, all with `@IntoSet`. Logout (`:feature:auth`) injects
`Set<UserDataCleaner>` and runs them all.

## Paging (data side)

Use Paging 3 only for unbounded lists; small lists keep `Flow<AppResult<List<T>>>`. Full procedure:
`.claude/skills/add-paging-list/`.
- Offline-first: a `RemoteMediator` in `data:core` writes pages into Room; the Room `PagingSource`
  from the feature's DAO is the source of truth.
- The repository returns `Flow<PagingData<XData>>`, not wrapped in `AppResult`.
- `RemoteMediator` errors are `MediatorResult.Error(AppErrorException(appError))`.
