package app.vigil.feature.settings.ui.appearance

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.mvi.UiState
import app.vigil.feature.settings.domain.model.ThemeMode

@Immutable
internal data class AppearanceUiState(
    /** Null until the stored choice has been read. */
    val selected: ThemeMode?,
    val effect: UiEffect<AppearanceAction>?,
) : UiState

internal sealed interface AppearanceAction : UiAction {
    data class Select(val mode: ThemeMode) : AppearanceAction

    data object Refresh : AppearanceAction

    class EffectConsumed(val effect: UiEffect<AppearanceAction>) : AppearanceAction
}
