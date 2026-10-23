package app.vigil.feature.auth.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import app.vigil.common.ui.mvi.BaseViewModel
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiNavigation
import app.vigil.common.ui.mvi.UiState
import app.vigil.feature.auth.domain.usecase.ObserveSessionExpiredUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Immutable
internal data class AuthSessionUiState(val navigation: AuthSessionNavigation?) : UiState

internal sealed interface AuthSessionAction : UiAction {
    data class NavigationConsumed(val navigation: AuthSessionNavigation) : AuthSessionAction
}

internal sealed interface AuthSessionNavigation : UiNavigation {
    data object SessionExpired : AuthSessionNavigation
}

/** Turns session expiry (raised by the network layer) into navigation to the login screen. */
@HiltViewModel
internal class AuthSessionViewModel @Inject constructor(observeSessionExpired: ObserveSessionExpiredUseCase) :
    BaseViewModel<AuthSessionUiState, AuthSessionAction, AuthSessionNavigation>() {

    init {
        observeSessionExpired()
            .onEach { navigate(AuthSessionNavigation.SessionExpired) }
            .launchIn(viewModelScope)
    }

    @Composable
    override fun uiState() = AuthSessionUiState(navigation = currentNavigation)

    override fun onAction(action: AuthSessionAction) {
        when (action) {
            is AuthSessionAction.NavigationConsumed -> consumeNavigation(action.navigation)
        }
    }
}
