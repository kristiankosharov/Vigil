package app.vigil.feature.auth.ui.login

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.mvi.UiNavigation
import app.vigil.common.ui.mvi.UiState

@Immutable
internal data class LoginUiState(
    val isLoggingIn: Boolean,
    val effect: UiEffect<LoginAction>?,
    val navigation: LoginNavigation?,
) : UiState

internal sealed interface LoginAction : UiAction {
    data object Submit : LoginAction

    class EffectConsumed(val effect: UiEffect<LoginAction>) : LoginAction

    data class NavigationConsumed(val navigation: LoginNavigation) : LoginAction
}

internal sealed interface LoginNavigation : UiNavigation {
    data object LoggedIn : LoginNavigation
}
