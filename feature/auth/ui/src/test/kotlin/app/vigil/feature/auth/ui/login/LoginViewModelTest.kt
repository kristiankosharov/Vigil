package app.vigil.feature.auth.ui.login

import androidx.lifecycle.SavedStateHandle
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.text.UiText
import app.vigil.feature.auth.domain.model.OidcProvider
import app.vigil.feature.auth.domain.model.PhoneVerification
import app.vigil.feature.auth.domain.model.SignInIdentifierKind
import app.vigil.feature.auth.domain.usecase.RequestPhoneCodeUseCase
import app.vigil.feature.auth.domain.usecase.SendPasswordResetUseCase
import app.vigil.feature.auth.domain.usecase.SignInWithEmailUseCase
import app.vigil.feature.auth.domain.usecase.SignUpWithEmailUseCase
import app.vigil.feature.auth.domain.usecase.VerifyPhoneCodeUseCase
import app.vigil.feature.auth.ui.R
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

private val NoConnection = AppResult.Failure(AppError.Network.NoConnection(null))

class LoginViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    @Suppress("LongParameterList") // a fake for every use case
    fun viewModel(
        signInWithEmail: SignInWithEmailUseCase = SignInWithEmailUseCase { _, _ -> NoConnection },
        signUpWithEmail: SignUpWithEmailUseCase = SignUpWithEmailUseCase { _, _ -> NoConnection },
        requestPhoneCode: RequestPhoneCodeUseCase = RequestPhoneCodeUseCase { flowOf(NoConnection) },
        verifyPhoneCode: VerifyPhoneCodeUseCase = VerifyPhoneCodeUseCase { _, _ -> NoConnection },
        signInWithOidc: (String) -> AppResult<Unit> = { NoConnection },
        oidcProviders: List<OidcProvider> = emptyList(),
        sendPasswordReset: SendPasswordResetUseCase = SendPasswordResetUseCase { AppResult.Success(Unit) },
    ) = LoginViewModel(
        savedStateHandle = SavedStateHandle(),
        signInWithEmail = signInWithEmail,
        signUpWithEmail = signUpWithEmail,
        sendPasswordReset = sendPasswordReset,
        requestPhoneCode = requestPhoneCode,
        verifyPhoneCode = verifyPhoneCode,
        signInWithOidc = { signInWithOidc(it) },
        getOidcProviders = { oidcProviders },
    )

    fun LoginViewModel.enterEmail(email: String = "ana@example.com", password: String = "secret1") {
        onAction(LoginAction.IdentifierChanged(email))
        onAction(LoginAction.PasswordChanged(password))
    }

    test("signing in with email navigates; NavigationConsumed clears the navigation") {
        val vm = viewModel(signInWithEmail = { email, password ->
            if (email == "ana@example.com" && password == "secret1") AppResult.Success(Unit) else NoConnection
        })

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.enterEmail(email = " ana@example.com ") // trimmed
            vm.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.navigation shouldBe LoginNavigation.LoggedIn
            state.isBusy shouldBe false

            vm.onAction(LoginAction.NavigationConsumed(LoginNavigation.LoggedIn))
            awaitItem().navigation shouldBe null
        }
    }

    test("an invalid email or short password shows an error without calling Firebase") {
        val signIn = mockk<SignInWithEmailUseCase>()
        val vm = viewModel(signInWithEmail = signIn)

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.enterEmail(email = "ana@")
            vm.onAction(LoginAction.Submit)
            expectMostRecentItem().error shouldBe UiText.Resource(R.string.auth_error_email_format)

            vm.enterEmail(password = "12345")
            vm.onAction(LoginAction.Submit)
            expectMostRecentItem().error.shouldBeInstanceOf<UiText.Plural>().id shouldBe
                R.plurals.auth_error_password_length

            vm.onAction(LoginAction.PasswordChanged("123456"))
            expectMostRecentItem().error shouldBe null // typing clears the error
        }
        coVerify(exactly = 0) { signIn(any(), any()) }
    }

    test("wrong credentials show the auth message and do not navigate") {
        val vm = viewModel(signInWithEmail = { _, _ -> AppResult.Failure(AppError.Auth.InvalidCredentials(null)) })

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.enterEmail()
            vm.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.error shouldBe UiText.Resource(R.string.auth_error_invalid_credentials)
            state.navigation shouldBe null
        }
    }

    test("in sign-up mode Submit creates the account") {
        val signUp = mockk<SignUpWithEmailUseCase>()
        coEvery { signUp(any(), any()) } returns AppResult.Success(Unit)
        val vm = viewModel(signUpWithEmail = signUp)

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.onAction(LoginAction.EmailModeToggled)
            vm.enterEmail()
            vm.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.emailMode shouldBe EmailMode.SignUp
            state.navigation shouldBe LoginNavigation.LoggedIn
        }
        coVerify(exactly = 1) { signUp("ana@example.com", "secret1") }
    }

    test("busy while signing in; a second Submit is ignored") {
        val signIn = mockk<SignInWithEmailUseCase>()
        coEvery { signIn(any(), any()) } coAnswers {
            delay(1_000)
            AppResult.Success(Unit)
        }
        val vm = viewModel(signInWithEmail = signIn)

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.enterEmail()
            vm.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.runCurrent()
            expectMostRecentItem().isBusy shouldBe true

            vm.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().isBusy shouldBe false
        }
        coVerify(exactly = 1) { signIn(any(), any()) }
    }

    test("forgot password sends the reset and shows a snackbar") {
        val vm = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.enterEmail()
            vm.onAction(LoginAction.ForgotPassword)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().effect.shouldBeInstanceOf<UiEffect.ShowSnackbar<LoginAction>>()
        }
    }

    test("phone: the code is sent, then the typed code signs in") {
        val verify = mockk<VerifyPhoneCodeUseCase>()
        coEvery { verify("id-1", "123456") } returns AppResult.Success(Unit)
        val vm = viewModel(
            requestPhoneCode = { flowOf(AppResult.Success(PhoneVerification.CodeSent("id-1"))) },
            verifyPhoneCode = verify,
        )

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.onAction(LoginAction.IdentifierChanged("+359 888 123 456"))
            vm.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().run {
                phoneNumber shouldBe "+359888123456"
                phoneStep shouldBe PhoneStep.EnterCode
                isBusy shouldBe false
            }

            vm.onAction(LoginAction.CodeChanged("12345a")) // ignored: not digits
            vm.onAction(LoginAction.CodeChanged("123456"))
            vm.onAction(LoginAction.VerifyCode)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().navigation shouldBe LoginNavigation.LoggedIn
        }
    }

    test("phone: an invalid number is rejected before sending") {
        val request = mockk<RequestPhoneCodeUseCase>()
        val vm = viewModel(requestPhoneCode = request)

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.onAction(LoginAction.IdentifierChanged("0888123456"))
            vm.onAction(LoginAction.Submit)
            expectMostRecentItem().error shouldBe UiText.Resource(R.string.auth_error_phone_format)
        }
        coVerify(exactly = 0) { request(any()) }
    }

    test("phone: automatic verification after the code was sent navigates without a typed code") {
        val vm = viewModel(requestPhoneCode = {
            flow {
                emit(AppResult.Success(PhoneVerification.CodeSent("id-1")))
                delay(5_000)
                emit(AppResult.Success(PhoneVerification.Verified))
            }
        })

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.onAction(LoginAction.IdentifierChanged("+359888123456"))
            vm.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().navigation shouldBe LoginNavigation.LoggedIn
        }
    }

    test("phone: a wrong code shows the code error; Change number goes back") {
        val vm = viewModel(
            requestPhoneCode = { flowOf(AppResult.Success(PhoneVerification.CodeSent("id-1"))) },
            verifyPhoneCode = { _, _ -> AppResult.Failure(AppError.Auth.InvalidCode(null)) },
        )

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.onAction(LoginAction.IdentifierChanged("+359888123456"))
            vm.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            vm.onAction(LoginAction.CodeChanged("000000"))
            vm.onAction(LoginAction.VerifyCode)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().error shouldBe UiText.Resource(R.string.auth_error_invalid_code)

            vm.onAction(LoginAction.ChangePhoneNumber)
            expectMostRecentItem().phoneStep shouldBe PhoneStep.EnterNumber
        }
    }

    test("OIDC: providers are listed; a cancelled sign-in shows no error") {
        val vm = viewModel(
            oidcProviders = listOf(OidcProvider("oidc.company", "Company SSO")),
            signInWithOidc = { AppResult.Failure(AppError.Auth.Cancelled(null)) },
        )

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            awaitItem().oidcProviders shouldBe listOf(OidcProviderItem("oidc.company", "Company SSO"))

            vm.onAction(LoginAction.OidcSelected("oidc.company"))
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.error shouldBe null
            state.navigation shouldBe null
        }
    }

    test("the field decides the method: an email needs a password, a number gets a code") {
        val vm = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            awaitItem().identifierKind shouldBe SignInIdentifierKind.Unknown

            vm.onAction(LoginAction.IdentifierChanged("ana@"))
            expectMostRecentItem().identifierKind shouldBe SignInIdentifierKind.Email

            vm.onAction(LoginAction.IdentifierChanged("+359 888"))
            expectMostRecentItem().run {
                identifierKind shouldBe SignInIdentifierKind.Phone
                identifier shouldBe "+359 888" // kept as typed; normalized only when sending
            }
        }
    }

    test("text that is neither an email nor a number shows an error and calls nothing") {
        val signIn = mockk<SignInWithEmailUseCase>()
        val request = mockk<RequestPhoneCodeUseCase>()
        val vm = viewModel(signInWithEmail = signIn, requestPhoneCode = request)

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.onAction(LoginAction.IdentifierChanged("ana"))
            vm.onAction(LoginAction.Submit)
            expectMostRecentItem().error shouldBe UiText.Resource(R.string.auth_error_identifier_format)
        }
        coVerify(exactly = 0) { signIn(any(), any()) }
        coVerify(exactly = 0) { request(any()) }
    }

    test("forgot password with a phone number asks for an email instead") {
        val reset = mockk<SendPasswordResetUseCase>()
        val vm = viewModel(sendPasswordReset = reset)

        moleculeFlow(RecompositionMode.Immediate) { vm.uiState() }.test {
            vm.onAction(LoginAction.IdentifierChanged("+359888123456"))
            vm.onAction(LoginAction.ForgotPassword)
            expectMostRecentItem().error shouldBe UiText.Resource(R.string.auth_error_email_format)
        }
        coVerify(exactly = 0) { reset(any()) }
    }
})
