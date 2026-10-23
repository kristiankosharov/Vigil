---
paths:
  - "feature/*/ui/**"
  - "common/ui/**"
---

# UI layer: MVI, ViewModel, effects, Compose

Code: `.claude/reference/mvi-base.md` (BaseViewModel, UiEffectQueue, UiEffect, handlers) and
`.claude/reference/screen-example.md` (a full screen). Procedure: `.claude/skills/new-screen/`.

## Pieces of a screen

| Piece | Type | Purpose |
|---|---|---|
| `XUiState` | `@Immutable data class` | Everything the screen renders. |
| `XAction` | `sealed interface : UiAction` | Every user intent the ViewModel handles. |
| `UiEffect<XAction>` | from `:common:ui` | One-shot toast/snackbar, exposed as `XUiState.effect`. |
| `XNavigation` | `sealed interface : UiNavigation` (optional) | Navigation after an async result, exposed as `XUiState.navigation`. |
| `XViewModel` | `@HiltViewModel`, extends `BaseViewModel<XUiState, XAction, XNavigation>` | Owns state fields, builds `XUiState`, handles actions, sends effects. |
| `XRoute` | `@Composable`, public | Pure wiring: ViewModel → state → screen. |
| `XScreen` | `@Composable`, internal, stateless | Renders state, emits actions and navigation callbacks. Has previews. |

## `:common:ui` provides

Theme and design system; `UiText` + `AppError.toUiText()`; marker interfaces `UiState`, `UiAction`,
`UiNavigation`; `BaseViewModel<S, A, N>`; `UiEffect`, `UiEffectQueue`, `UiEffectHandler`;
`NavigationHandler`.

## UiState

- `@Immutable data class` with `val`s only. Collections are `ImmutableList` / `ImmutableMap` /
  `ImmutableSet`. The only exception is `LazyPagingItems` for paged lists.
- Only domain models or UI models (`*Item`), never `*Data` models, never `AppError` (turn it into a
  `UiText` for persistent error UI, or a `UiEffect`).
- `effect: UiEffect<XAction>?` — the effect waiting to be shown. `navigation: XNavigation?` — only on
  screens that navigate after an async result.

## Actions

- The screen calls nothing on the ViewModel except `onAction`.
- Screens with effects have `EffectConsumed(effect)`; screens with `XNavigation` have
  `NavigationConsumed(navigation)`.
- Actions, effects and navigation contain only data — **no function types**. They are stored in the
  ViewModel, so a lambda would leak what it captures.

## ViewModel — state built by a `@Composable` function

- No `StateFlow` / `MutableStateFlow` for UI state; no `Channel` / `SharedFlow` for events.
- Mutable state is **private Compose snapshot fields** (`private var x by mutableStateOf(...)`).
  They survive configuration changes.
- **Process death:** fields the user typed or chose (query, form input, selected tab) use
  `savedStateHandle.saveable { mutableStateOf(...) }`. Reloadable data stays plain `mutableStateOf`.
  Secrets (PIN, password) are **never** saveable: they would be written to the saved instance state.
- `@Composable override fun uiState(): XUiState` builds the immutable state from the fields. It must
  be pure: read fields and derive values (`remember(key) { … }` for expensive derivations); no side
  effects, no `LaunchedEffect`, no use case calls.
- `override fun onAction(action)` is the single public entry point, an exhaustive `when` that changes
  the private fields.
- Long-lived data from use cases is collected in `viewModelScope`, started in `init`. These
  collections keep running in the background for the ViewModel's lifetime (accepted: Room flows are
  cheap). Never poll the network from them.
- **No overlapping work:** keep the `Job` of repeatable operations (refresh, load more) and ignore new
  requests while it is active; reset loading flags in `finally`.
- ViewModels depend only on use case interfaces — never repositories, APIs, DAOs or `Context`.
- **Threading:** snapshot fields are main-thread only; every coroutine writing them runs in
  `viewModelScope` (`Main.immediate`). Switch threads only inside use cases/repositories.
- **Arguments:** a ViewModel that needs a navigation argument uses Hilt assisted injection
  (`@HiltViewModel(assistedFactory = …)`, `@AssistedInject`, `@Assisted`).

## Effects (toast, snackbar)

- `sendEffect(...)` enqueues; `uiState()` exposes `currentEffect` as `effect`;
  `EffectConsumed(effect)` calls `consumeEffect(effect)` so the next one is shown. The queue lives in
  the ViewModel, so effects survive configuration changes and backgrounding.
- `UiEffectQueue` is a FIFO backed by an immutable `PersistentList` in snapshot state. It is the one
  structure safe from any thread: updates run under a coroutine `Mutex` (never `synchronized`).
  `consume(effect)` removes the effect only if it is still the head (identity check).
- Feature ViewModels never touch the queue directly; only `BaseViewModel` uses `UiEffectQueue`.
- Effects are plain classes (identity equality) so the same message sent twice is shown twice.
- Messages are `UiText`, never hardcoded strings or `Context`. `UiText` holds only plain data
  (`Dynamic(String)`, `Resource(id, args)`, `Plural(id, count, args)`) — never a `Context`,
  `Resources`, `View` or lambda.
- An effect is consumed only after it is fully shown. A snackbar interrupted by rotation or
  navigation is shown again later (intended). Toasts use `context.applicationContext`.
- Only `UiEffectHandler` in `:common:ui` calls `Toast.makeText` or `showSnackbar`.

## Navigation

- Navigation is never a `UiEffect`.
- Pure navigation (e.g. item click) is a **callback parameter of `XScreen`**, wired straight to the
  Route's navigation lambda; it doesn't go through the ViewModel.
- Navigation after an async result is **consumable state**: the ViewModel calls
  `navigate(XNavigation.Back)`, and `XScreen` handles `state.navigation` with `NavigationHandler`,
  which consumes **before** navigating. Never pass a callback into the ViewModel or an action.

## Compose

- **`XRoute` contains no logic:** `hiltViewModel()`, `viewModel.uiState()`, then `XScreen(state,
  viewModel::onAction, navigation lambdas)`. No `LaunchedEffect`, `DisposableEffect`, `SideEffect`,
  `remember`, `if`/`when`, or lambdas with bodies. Only exception: the assisted-injection
  `creationCallback` passed to `hiltViewModel`.
- `XScreen` is stateless: `(state, onAction, <navigation callbacks>, modifier: Modifier = Modifier)`,
  with `@Preview`s using fixed state. It owns the `Scaffold` and `SnackbarHostState`, calls
  `UiEffectHandler`, and `NavigationHandler` if it has `XNavigation`.
- Other `LaunchedEffect`s only for purely visual concerns (scroll, focus, animation) — never for
  loading data, showing messages or navigation.
- Small composables take plain parameters, not the whole state or the ViewModel. Every composable
  that emits UI takes `modifier: Modifier = Modifier` as the first optional parameter.

## Paging (UI side)

- Keep a private `Flow<PagingData<XItem>>` built once with `.cachedIn(viewModelScope)`; `uiState()`
  collects it with `collectAsLazyPagingItems()` into the state.
- Render with `LazyColumn` + `items(count = items.itemCount, key = items.itemKey { it.id })`; show
  loading/error rows from `items.loadState`; retry with `items.retry()`. Errors come back as
  `AppErrorException` in `LoadState.Error`, mapped with `toUiText()`.
