---
name: new-screen
description: Create a new MVI screen in a feature ui module (UiState, Action, optional Navigation, ViewModel with @Composable uiState, stateless Screen, logic-free Route, previews, tests). Use when the user asks to add a screen, page, view or dialog.
---

# New screen `Bar` in `:feature:foo:ui`

Rules: `.claude/rules/ui-mvi.md` and `.claude/rules/ui-quality.md` (auto-loaded in ui modules).
Full worked example: `.claude/reference/screen-example.md`. Base types:
`.claude/reference/mvi-base.md` (or the real files in `:common:ui`).

## Steps

1. **`BarUiState`** — `@Immutable internal data class … : UiState`, `val`s only, immutable
   collections, `effect: UiEffect<BarAction>?`, plus `navigation: BarNavigation?` if the screen
   navigates after an async result.
2. **`BarAction`** — `internal sealed interface … : UiAction`, data only (no lambdas), including
   `class EffectConsumed(val effect: UiEffect<BarAction>)` and, if needed,
   `data class NavigationConsumed(val navigation: BarNavigation)`.
3. **`BarNavigation`** (optional) — `internal sealed interface … : UiNavigation`, data only.
4. **`BarViewModel`** — `@HiltViewModel internal class … : BaseViewModel<BarUiState, BarAction, BarNavigation>()`
   (`Nothing` if no result navigation):
   - private snapshot fields; user input via `savedStateHandle.saveable { mutableStateOf(…) }`;
   - long-lived use case flows collected in `init` in `viewModelScope`;
   - `@Composable override fun uiState()` — pure, reads fields, `remember` for derivations, exposes
     `currentEffect` / `currentNavigation`;
   - `override fun onAction` — exhaustive `when`; `consumeEffect(action.effect)`,
     `consumeNavigation(action.navigation)`;
   - repeatable work guarded by a `Job` and `finally`;
   - needs a navigation argument → assisted injection (`@HiltViewModel(assistedFactory = …)`).
5. **`BarScreen`** — `internal`, stateless `(state, onAction, <navigation callbacks>, modifier)`;
   owns `Scaffold` + `SnackbarHostState`; calls `UiEffectHandler(…, onEffectConsumed = { onAction(BarAction.EffectConsumed(it)) })`
   and `NavigationHandler` if needed; `@PreviewLightDark` + `@PreviewFontScale` previews with fixed state.
6. **`BarRoute`** — the only public composable: `hiltViewModel()` (or `hiltViewModel<VM, Factory> { it.create(arg) }`),
   `viewModel.uiState()`, `BarScreen(...)`. No other logic.
7. **Strings** prefixed `foo_` in the feature's `strings.xml`; accessibility per `ui-quality.md`.
8. **Wire it** in `:app`: a `@Serializable` `NavKey` and an `entry<…>` (`.claude/rules/app.md`).
9. **Tests:** Kotest + Molecule + Turbine for the ViewModel (every action, every result path, every
   effect/navigation); a Compose UI test for `BarScreen` with accessibility checks. Run
   `scripts/check.sh tests :feature:foo:ui` and `scripts/check.sh android :feature:foo:ui`.

## Skeleton

`templates/BarScreen.kt` — UiState, Action, ViewModel, Route, stateless Screen and previews for a
minimal screen. Copy it, rename `Bar`, then fill in the steps above.
