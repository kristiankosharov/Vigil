package app.vigil.feature.auth.ui.login

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.mvi.UiNavigation
import app.vigil.common.ui.mvi.UiState
import app.vigil.common.ui.text.UiText
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class LoginUiState(
    val method: LoginMethod,
    val emailMode: EmailMode,
    val email: String,
    val password: String,
    val phoneStep: PhoneStep,
    val phoneNumber: String,
    val code: String,
    val oidcProviders: ImmutableList<OidcProviderItem>,
    /** Shown under the form until the input changes. */
    val error: UiText?,
    val isBusy: Boolean,
    val effect: UiEffect<LoginAction>?,
    val navigation: LoginNavigation?,
) : UiState

internal enum class LoginMethod { Email, Phone }

internal enum class EmailMode { SignIn, SignUp }

internal enum class PhoneStep { EnterNumber, EnterCode }

@Immutable
internal data class OidcProviderItem(val id: String, val displayName: String)

internal sealed interface LoginAction : UiAction {
    data class MethodSelected(val method: LoginMethod) : LoginAction

    data class EmailChanged(val email: String) : LoginAction

    data class PasswordChanged(val password: String) : LoginAction

    data object EmailModeToggled : LoginAction

    data object SubmitEmail : LoginAction

    data object ForgotPassword : LoginAction

    data class PhoneNumberChanged(val phoneNumber: String) : LoginAction

    data object SendCode : LoginAction

    data class CodeChanged(val code: String) : LoginAction

    data object VerifyCode : LoginAction

    data object ChangePhoneNumber : LoginAction

    data class OidcSelected(val providerId: String) : LoginAction

    class EffectConsumed(val effect: UiEffect<LoginAction>) : LoginAction

    data class NavigationConsumed(val navigation: LoginNavigation) : LoginAction
}

internal sealed interface LoginNavigation : UiNavigation {
    data object LoggedIn : LoginNavigation
}
