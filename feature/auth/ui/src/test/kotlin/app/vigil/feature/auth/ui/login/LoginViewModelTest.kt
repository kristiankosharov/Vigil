package app.vigil.feature.auth.ui.login

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.feature.auth.domain.usecase.LoginUseCase
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay

class LoginViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    test("Submit logs in and navigates; NavigationConsumed clears the navigation") {
        val viewModel = LoginViewModel(login = { AppResult.Success(Unit) })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().navigation shouldBe null

            viewModel.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.navigation shouldBe LoginNavigation.LoggedIn
            state.isLoggingIn shouldBe false

            viewModel.onAction(LoginAction.NavigationConsumed(LoginNavigation.LoggedIn))
            awaitItem().navigation shouldBe null
        }
    }

    test("a failed login shows a snackbar and does not navigate") {
        val viewModel = LoginViewModel(login = { AppResult.Failure(AppError.Network.NoConnection(null)) })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.effect.shouldBeInstanceOf<UiEffect.ShowSnackbar<LoginAction>>()
            state.navigation shouldBe null
        }
    }

    test("isLoggingIn while the login runs; a second Submit is ignored") {
        val login = mockk<LoginUseCase>()
        coEvery { login() } coAnswers {
            delay(1_000)
            AppResult.Success(Unit)
        }
        val viewModel = LoginViewModel(login)

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.runCurrent()
            expectMostRecentItem().isLoggingIn shouldBe true

            viewModel.onAction(LoginAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().isLoggingIn shouldBe false
        }
        coVerify(exactly = 1) { login() }
    }
})
