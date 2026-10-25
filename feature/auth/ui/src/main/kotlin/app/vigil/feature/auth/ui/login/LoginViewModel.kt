package app.vigil.feature.auth.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.ui.mvi.BaseViewModel
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.text.UiText
import app.vigil.common.ui.text.toUiText
import app.vigil.feature.auth.domain.model.AuthRules
import app.vigil.feature.auth.domain.model.PhoneVerification
import app.vigil.feature.auth.domain.usecase.GetOidcProvidersUseCase
import app.vigil.feature.auth.domain.usecase.RequestPhoneCodeUseCase
import app.vigil.feature.auth.domain.usecase.SendPasswordResetUseCase
import app.vigil.feature.auth.domain.usecase.SignInWithEmailUseCase
import app.vigil.feature.auth.domain.usecase.SignInWithOidcUseCase
import app.vigil.feature.auth.domain.usecase.SignUpWithEmailUseCase
import app.vigil.feature.auth.domain.usecase.VerifyPhoneCodeUseCase
import app.vigil.feature.auth.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
internal class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val signInWithEmail: SignInWithEmailUseCase,
    private val signUpWithEmail: SignUpWithEmailUseCase,
    private val sendPasswordReset: SendPasswordResetUseCase,
    private val requestPhoneCode: RequestPhoneCodeUseCase,
    private val verifyPhoneCode: VerifyPhoneCodeUseCase,
    private val signInWithOidc: SignInWithOidcUseCase,
    getOidcProviders: GetOidcProvidersUseCase,
) : BaseViewModel<LoginUiState, LoginAction, LoginNavigation>() {

    private var method by savedStateHandle.saveable { mutableStateOf(LoginMethod.Email) }
    private var emailMode by savedStateHandle.saveable { mutableStateOf(EmailMode.SignIn) }
    private var email by savedStateHandle.saveable { mutableStateOf("") }
    private var phoneNumber by savedStateHandle.saveable { mutableStateOf("") }

    // Never savedStateHandle: secrets must not be written to the saved instance state. After process
    // death the phone flow starts again from the number.
    private var password by mutableStateOf("")
    private var code by mutableStateOf("")
    private var verificationId by mutableStateOf<String?>(null)

    private val oidcProviders = getOidcProviders().map { OidcProviderItem(it.id, it.displayName) }.toImmutableList()
    private var error by mutableStateOf<UiText?>(null)
    private var isBusy by mutableStateOf(false)
    private var job: Job? = null // one sign-in request at a time

    @Composable
    override fun uiState() = LoginUiState(
        method = method,
        emailMode = emailMode,
        email = email,
        password = password,
        phoneStep = if (verificationId == null) PhoneStep.EnterNumber else PhoneStep.EnterCode,
        phoneNumber = phoneNumber,
        code = code,
        oidcProviders = oidcProviders,
        error = error,
        isBusy = isBusy,
        effect = currentEffect,
        navigation = currentNavigation,
    )

    override fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.MethodSelected -> edit { method = action.method }
            is LoginAction.EmailChanged -> edit { email = action.email.trim() }
            is LoginAction.PasswordChanged -> edit { password = action.password }
            LoginAction.EmailModeToggled -> edit { emailMode = emailMode.toggled() }
            LoginAction.SubmitEmail -> submitEmail()
            LoginAction.ForgotPassword -> resetPassword()
            is LoginAction.PhoneNumberChanged -> edit { phoneNumber = action.phoneNumber.filter(::isPhoneChar) }
            LoginAction.SendCode -> sendCode()
            is LoginAction.CodeChanged -> changeCode(action.code)
            LoginAction.VerifyCode -> verifyCode()
            LoginAction.ChangePhoneNumber -> edit { verificationId = null }
            is LoginAction.OidcSelected -> launchRequest { signInWithOidc(action.providerId) }
            is LoginAction.EffectConsumed -> consumeEffect(action.effect)
            is LoginAction.NavigationConsumed -> consumeNavigation(action.navigation)
        }
    }

    /** Input changes are ignored while a request runs, and clear the last error. */
    private inline fun edit(change: () -> Unit) {
        if (isBusy) return
        change()
        error = null
    }

    private fun changeCode(input: String) {
        if (input.length <= AuthRules.PHONE_CODE_LENGTH && input.all(Char::isDigit)) edit { code = input }
    }

    private fun submitEmail() {
        error = when {
            !AuthRules.isValidEmail(email) -> UiText.Resource(R.string.auth_error_email_format)

            !AuthRules.isValidPassword(password) -> UiText.Plural(
                id = R.plurals.auth_error_password_length,
                count = AuthRules.PASSWORD_MIN_LENGTH,
                args = listOf(AuthRules.PASSWORD_MIN_LENGTH),
            )

            else -> null
        }
        if (error != null) return
        when (emailMode) {
            EmailMode.SignIn -> launchRequest { signInWithEmail(email, password) }
            EmailMode.SignUp -> launchRequest { signUpWithEmail(email, password) }
        }
    }

    private fun resetPassword() {
        if (!AuthRules.isValidEmail(email)) {
            error = UiText.Resource(R.string.auth_error_email_format)
            return
        }
        launchRequest(onSuccess = { sendEffect(UiEffect.ShowSnackbar(UiText.Resource(R.string.auth_reset_sent))) }) {
            sendPasswordReset(email)
        }
    }

    private fun sendCode() {
        if (job?.isActive == true) return
        if (!AuthRules.isValidPhoneNumber(phoneNumber)) {
            error = UiText.Resource(R.string.auth_error_phone_format)
            return
        }
        isBusy = true
        error = null
        code = ""
        job = requestPhoneCode(phoneNumber)
            .onEach { result ->
                when (result) {
                    is AppResult.Success -> when (val verification = result.data) {
                        is PhoneVerification.CodeSent -> {
                            verificationId = verification.verificationId
                            isBusy = false
                        }

                        PhoneVerification.Verified -> navigate(LoginNavigation.LoggedIn)
                    }

                    is AppResult.Failure -> showError(result.error)
                }
            }
            .onCompletion { isBusy = false }
            .launchIn(viewModelScope)
    }

    private fun verifyCode() {
        val id = verificationId ?: return
        if (!AuthRules.isValidPhoneCode(code)) {
            error = UiText.Plural(
                id = R.plurals.auth_error_code_format,
                count = AuthRules.PHONE_CODE_LENGTH,
                args = listOf(AuthRules.PHONE_CODE_LENGTH),
            )
            return
        }
        // Replaces the code request, whose auto-verification could otherwise sign in a second time.
        job?.cancel()
        isBusy = false
        launchRequest { verifyPhoneCode(id, code) }
    }

    /** Runs one sign-in request: busy while it runs; navigates on success unless [onSuccess] says otherwise. */
    private fun launchRequest(
        onSuccess: () -> Unit = { navigate(LoginNavigation.LoggedIn) },
        request: suspend () -> AppResult<Unit>,
    ) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            isBusy = true
            error = null
            try {
                when (val result = request()) {
                    is AppResult.Success -> onSuccess()
                    is AppResult.Failure -> showError(result.error)
                }
            } finally {
                isBusy = false
            }
        }
    }

    private fun showError(appError: AppError) {
        if (appError is AppError.Auth.Cancelled) return // the user backed out of the provider's page
        error = appError.toLoginText()
    }
}

private fun isPhoneChar(char: Char) = char == '+' || char.isDigit()

private fun EmailMode.toggled() = if (this == EmailMode.SignIn) EmailMode.SignUp else EmailMode.SignIn

private fun AppError.toLoginText(): UiText = when (this) {
    is AppError.Auth.InvalidCredentials -> UiText.Resource(R.string.auth_error_invalid_credentials)
    is AppError.Auth.AccountExists -> UiText.Resource(R.string.auth_error_account_exists)
    is AppError.Auth.WeakPassword -> UiText.Resource(R.string.auth_error_weak_password)
    is AppError.Auth.InvalidPhoneNumber -> UiText.Resource(R.string.auth_error_phone_format)
    is AppError.Auth.InvalidCode -> UiText.Resource(R.string.auth_error_invalid_code)
    is AppError.Auth.TooManyRequests -> UiText.Resource(R.string.auth_error_too_many_requests)
    else -> toUiText()
}
