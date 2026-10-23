package app.vigil.common.ui.mvi

/** Marker for a screen's `@Immutable` state (`XUiState`). */
interface UiState

/** Marker for a screen's user intents (`XAction`). Data only: never function types. */
interface UiAction

/** Marker for navigation that happens after an async result (`XNavigation`). Data only. */
interface UiNavigation
