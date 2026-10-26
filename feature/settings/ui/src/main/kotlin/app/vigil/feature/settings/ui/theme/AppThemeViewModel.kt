package app.vigil.feature.settings.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import app.vigil.common.core.onSuccess
import app.vigil.common.ui.mvi.BaseViewModel
import app.vigil.feature.settings.domain.model.ThemeMode
import app.vigil.feature.settings.domain.usecase.ObserveThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Activity-scoped (AppThemeHost sits above navigation). Until the stored choice is read, and if it
 * can't be read, the app follows the system theme. A failed read is already logged by the store
 * wrapper and retried by its flow; there is nothing to show the user above the whole app.
 */
@HiltViewModel
internal class AppThemeViewModel @Inject constructor(observeThemeMode: ObserveThemeModeUseCase) :
    BaseViewModel<AppThemeUiState, AppThemeAction, Nothing>() {

    private var themeMode by mutableStateOf(ThemeMode.System)

    init {
        observeThemeMode()
            .onEach { result -> result.onSuccess { themeMode = it } }
            .launchIn(viewModelScope)
    }

    @Composable
    override fun uiState() = AppThemeUiState(themeMode = themeMode)

    override fun onAction(action: AppThemeAction) = Unit
}
