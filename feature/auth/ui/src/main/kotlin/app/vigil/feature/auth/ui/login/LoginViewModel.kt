package app.vigil.feature.auth.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import app.vigil.common.core.onFailure
import app.vigil.common.core.onSuccess
import app.vigil.common.ui.mvi.BaseViewModel
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.text.toUiText
import app.vigil.feature.auth.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// TODO(auth): the email/password fields (savedStateHandle.saveable) and their validation.
@HiltViewModel
internal class LoginViewModel @Inject constructor(private val login: LoginUseCase) :
    BaseViewModel<LoginUiState, LoginAction, LoginNavigation>() {

    private var isLoggingIn by mutableStateOf(false)
    private var loginJob: Job? = null

    @Composable
    override fun uiState() = LoginUiState(
        isLoggingIn = isLoggingIn,
        effect = currentEffect,
        navigation = currentNavigation,
    )

    override fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.Submit -> submit()
            is LoginAction.EffectConsumed -> consumeEffect(action.effect)
            is LoginAction.NavigationConsumed -> consumeNavigation(action.navigation)
        }
    }

    private fun submit() {
        if (loginJob?.isActive == true) return
        loginJob = viewModelScope.launch {
            isLoggingIn = true
            try {
                login()
                    .onSuccess { navigate(LoginNavigation.LoggedIn) }
                    .onFailure { error -> sendEffect(UiEffect.ShowSnackbar(error.toUiText())) }
            } finally {
                isLoggingIn = false
            }
        }
    }
}
