package app.vigil.feature.settings.ui.appearance

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
import app.vigil.feature.settings.domain.model.ThemeMode
import app.vigil.feature.settings.domain.usecase.ObserveThemeModeUseCase
import app.vigil.feature.settings.domain.usecase.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** The selection comes from the store, so the whole app (AppThemeHost) and this screen always agree. */
@HiltViewModel
internal class AppearanceViewModel @Inject constructor(
    private val observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : BaseViewModel<AppearanceUiState, AppearanceAction, Nothing>() {

    private var selected by mutableStateOf<ThemeMode?>(null)
    private var observeJob: Job? = null

    init {
        observe()
    }

    @Composable
    override fun uiState() = AppearanceUiState(selected = selected, effect = currentEffect)

    override fun onAction(action: AppearanceAction) {
        when (action) {
            is AppearanceAction.Select -> select(action.mode)
            AppearanceAction.Refresh -> observe()
            is AppearanceAction.EffectConsumed -> consumeEffect(action.effect)
        }
    }

    private fun observe() {
        if (observeJob?.isActive == true) return
        observeJob = observeThemeMode()
            .onEach { result ->
                result
                    .onSuccess { selected = it }
                    .onFailure { error ->
                        sendEffect(
                            UiEffect.ShowSnackbar(
                                message = error.toUiText(),
                                actionLabel = UiText.Resource(CommonR.string.common_retry),
                                action = AppearanceAction.Refresh,
                            ),
                        )
                    }
            }
            .launchIn(viewModelScope)
    }

    private fun select(mode: ThemeMode) {
        if (mode == selected) return
        viewModelScope.launch {
            setThemeMode(mode).onFailure { error -> sendEffect(UiEffect.ShowSnackbar(error.toUiText())) }
        }
    }
}
