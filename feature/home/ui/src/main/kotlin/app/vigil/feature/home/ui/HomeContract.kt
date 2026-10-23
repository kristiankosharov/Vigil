package app.vigil.feature.home.ui

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.mvi.UiState
import app.vigil.feature.home.domain.model.HomeEntry
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class HomeUiState(
    val isLoading: Boolean,
    val entries: ImmutableList<HomeEntryItem>,
    val effect: UiEffect<HomeAction>?,
) : UiState

@Immutable
internal data class HomeEntryItem(val id: String, val title: String)

internal fun HomeEntry.toItem() = HomeEntryItem(id = id, title = title)

internal sealed interface HomeAction : UiAction {
    data object Refresh : HomeAction

    class EffectConsumed(val effect: UiEffect<HomeAction>) : HomeAction
}
