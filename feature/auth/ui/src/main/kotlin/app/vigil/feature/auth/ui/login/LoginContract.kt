package app.vigil.feature.auth.ui.login

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.mvi.UiNavigation
import app.vigil.common.ui.mvi.UiState
import app.vigil.common.ui.text.UiText
import app.vigil.feature.auth.domain.model.SignInIdentifierKind
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class LoginUiState(
    /** One "email or phone number" field; [identifierKind] decides what the form asks next. */
    val identifier: String,
    val identifierKind: SignInIdentifierKind,
    val emailMode: EmailMode,
    val password: String,
    val phoneStep: PhoneStep,
    /** The normalized number the code was sent to (only meaningful in [PhoneStep.EnterCode]). */
    val phoneNumber: String,
    val code: String,
    val oidcProviders: ImmutableList<OidcProviderItem>,
    /** Shown under the form until the input changes. */
    val error: UiText?,
    val isBusy: Boolean,
    val effect: UiEffect<LoginAction>?,
    val navigation: LoginNavigation?,
) : UiState

internal enum class EmailMode { SignIn, SignUp }

internal enum class PhoneStep { EnterNumber, EnterCode }

@Immutable
internal data class OidcProviderItem(val id: String, val displayName: String)

internal sealed interface LoginAction : UiAction {
    data class IdentifierChanged(val identifier: String) : LoginAction

    data class PasswordChanged(val password: String) : LoginAction

    data object EmailModeToggled : LoginAction

    /** Sign in / create the account for an email, or send the SMS code for a phone number. */
    data object Submit : LoginAction

    data object ForgotPassword : LoginAction

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
