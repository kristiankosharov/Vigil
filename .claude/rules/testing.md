---
paths:
  - "**/src/test/**"
  - "**/src/androidTest/**"
  - "konsist/**"
  - "common/testing/**"
---

# Testing

Examples: `.claude/reference/tests.md`. Konsist rules: `.claude/reference/konsist-rules.md` (once
the `:konsist` module exists, its source is the truth).

## Tools

- **Kotest** for all unit tests (default style `FunSpec`), with Kotest matchers (`shouldBe`,
  `shouldBeInstanceOf`), never JUnit asserts.
- **MockK** (`mockk`, `coEvery`, `coVerify`); clear mocks in `afterTest { clearAllMocks() }` or create
  them per test. SAM use cases can be stubbed with a plain lambda. Prefer real fakes over deep mocks.
- **Turbine** (`test { awaitItem() }`) for every `Flow` assertion.
- **Molecule** (test only) turns a ViewModel's `@Composable uiState()` into a `Flow`:
  `moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test { … }`. If a test doesn't see
  a snapshot write, call `Snapshot.sendApplyNotifications()`. Android unit tests run with
  `unitTests.isReturnDefaultValues = true` (set by the convention plugin), because Compose calls
  `android.os.Trace` and `Log`.
- Coroutines: `Dispatchers.Main` is replaced by the shared Kotest extension in `:common:testing`
  (wraps a `StandardTestDispatcher`). Nothing launched in `init` runs until the scheduler is advanced:
  advance first, then read the latest item.
- **Compose UI tests** use `createComposeRule()` in `src/androidTest` (JUnit4; Kotest doesn't run as
  instrumentation). They test stateless `XScreen`s with fixed state, assert that `onAction` /
  navigation callbacks fire, and enable accessibility checks.

## What to test

- **Repositories:** mock `*Api`, `*Dao`, `DataStore<T>`; assert success and each `AppError` mapping
  (timeout, HTTP 4xx/5xx with body, serialization, DB failure, NotFound). `*ApiImpl` with Ktor
  `MockEngine`.
- **`*UseCaseImpl`:** mock repositories. SAM use cases need no tests.
- **ViewModels:** every `Action`, every use case result path, and the `UiEffect` / `XNavigation`
  each path produces.
- **`UiEffectQueue`:** concurrent sends from several coroutines on `Dispatchers.Default` lose nothing;
  `consume(effect)` drains in FIFO order; consuming a non-head effect is a no-op.
- **`asRetryingResult()`:** a failure emits `Failure`, re-subscribes and emits `Success` again once
  the source recovers; it ends after `maxRetries` **consecutive** failures; a success resets the
  counter (with `maxRetries = 3`: fail, fail, fail, succeed, fail, fail, fail keeps it alive; four
  failures in a row end it); two collectors keep separate counters.
- **Room migrations:** every `Migration_<from>_<to>` and auto-migration with `MigrationTestHelper`
  (instrumented).
- **DataStore stores:** on the JVM with a temporary file and a test `Aead` from a freshly generated
  Tink keyset. Test defaults, updates, a file written by an older model version (new fields get
  defaults), and that a file encrypted with another key or another file name resets to defaults.
- **`LazyAead`:** a setup throwing `GeneralSecurityException` or `ProviderException` surfaces as
  `IOException` → `AppError.Local.Storage`, the file is not reset, and the next access retries.
- **Paging:** test repositories/use cases with `paging-testing`'s `asSnapshot()`; test paged screens
  with Compose UI tests (`collectAsLazyPagingItems()` needs Android's main looper, so it doesn't run in
  JVM Molecule tests).

## Running tests

- Unit tests: `scripts/check.sh tests :module …`. On failure it prints each failing test with its
  message (read from the JUnit XML reports); never run raw Gradle test tasks.
- Instrumented tests (Compose UI tests, Room migrations): `scripts/check.sh android :module …` with
  an emulator running. Without a device it prints that the tests did **not** run; say so in your
  summary instead of claiming they passed.
- Konsist runs automatically (Stop hook) when you changed Kotlin files in this session; a failing
  rule means the code is wrong, not the test. When a new convention is added, add a Konsist rule
  for it.
