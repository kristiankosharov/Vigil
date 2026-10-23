package app.vigil.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import app.vigil.common.core.onFailure
import app.vigil.common.core.onSuccess
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.mvi.BaseViewModel
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.text.UiText
import app.vigil.common.ui.text.toUiText
import app.vigil.feature.home.domain.model.HomeEntry
import app.vigil.feature.home.domain.usecase.ObserveHomeEntriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
internal class HomeViewModel @Inject constructor(private val observeEntries: ObserveHomeEntriesUseCase) :
    BaseViewModel<HomeUiState, HomeAction, Nothing>() {

    private var isLoading by mutableStateOf(true)
    private var entries by mutableStateOf<ImmutableList<HomeEntryItem>>(persistentListOf())
    private var observeJob: Job? = null

    init {
        observe()
    }

    @Composable
    override fun uiState() = HomeUiState(isLoading = isLoading, entries = entries, effect = currentEffect)

    override fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Refresh -> observe()
            is HomeAction.EffectConsumed -> consumeEffect(action.effect)
        }
    }

    /** Subscribes to the stored entries; after the flow gave up (repeated failures), Refresh subscribes again. */
    private fun observe() {
        if (observeJob?.isActive == true) return
        // TODO(home): also refresh the entries from the API here once the backend exists.
        observeJob = observeEntries()
            .onEach { result ->
                isLoading = false
                result
                    .onSuccess { data -> entries = data.map(HomeEntry::toItem).toImmutableList() }
                    .onFailure { error ->
                        sendEffect(
                            UiEffect.ShowSnackbar(
                                message = error.toUiText(),
                                actionLabel = UiText.Resource(CommonR.string.common_retry),
                                action = HomeAction.Refresh,
                            ),
                        )
                    }
            }
            .launchIn(viewModelScope)
    }
}
