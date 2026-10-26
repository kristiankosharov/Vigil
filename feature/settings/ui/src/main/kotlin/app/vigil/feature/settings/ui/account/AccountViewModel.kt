package app.vigil.feature.settings.ui.account

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
 * A failed read is already logged by the store wrapper and retried by its flow; the row then just
 * shows no current value, and the Appearance screen reports the error.
 */
@HiltViewModel
internal class AccountViewModel @Inject constructor(observeThemeMode: ObserveThemeModeUseCase) :
    BaseViewModel<AccountUiState, AccountAction, Nothing>() {

    private var themeMode by mutableStateOf<ThemeMode?>(null)

    init {
        observeThemeMode()
            .onEach { result -> result.onSuccess { themeMode = it } }
            .launchIn(viewModelScope)
    }

    @Composable
    override fun uiState() = AccountUiState(themeMode = themeMode)

    override fun onAction(action: AccountAction) = Unit
}
