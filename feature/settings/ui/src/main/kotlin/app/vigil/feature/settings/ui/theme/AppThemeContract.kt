package app.vigil.feature.settings.ui.theme

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiState
import app.vigil.feature.settings.domain.model.ThemeMode

@Immutable
internal data class AppThemeUiState(val themeMode: ThemeMode) : UiState

/** The theme host only observes; it has no user intents. */
internal sealed interface AppThemeAction : UiAction
