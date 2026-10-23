package app.vigil.feature.applock.ui.create

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.common.ui.text.UiText
import app.vigil.feature.applock.ui.R
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CreatePinViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    test("a too short PIN shows the format error and saves nothing") {
        var saved = 0
        val viewModel = CreatePinViewModel(
            createPin = { _, _ -> AppResult.Success(Unit).also { saved++ } },
            isBiometricAvailable = { false },
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.onAction(CreatePinAction.PinChanged("12"))
            viewModel.onAction(CreatePinAction.ConfirmPinChanged("12"))
            viewModel.onAction(CreatePinAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().error shouldBe UiText.Plural(R.plurals.applock_error_pin_format, 8, listOf(4, 8))
        }
        saved shouldBe 0
    }

    test("PINs that don't match show the mismatch error") {
        val viewModel =
            CreatePinViewModel(createPin = { _, _ -> AppResult.Success(Unit) }, isBiometricAvailable = { false })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.onAction(CreatePinAction.PinChanged("1234"))
            viewModel.onAction(CreatePinAction.ConfirmPinChanged("1235"))
            viewModel.onAction(CreatePinAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().error shouldBe UiText.Resource(R.string.applock_error_pin_mismatch)
        }
    }

    test("a valid PIN is saved with the biometric choice, then navigates") {
        var savedPin: String? = null
        var savedBiometric: Boolean? = null
        val viewModel = CreatePinViewModel(
            createPin = { pin, biometric ->
                savedPin = pin
                savedBiometric = biometric
                AppResult.Success(Unit)
            },
            isBiometricAvailable = { true },
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().isBiometricEnabled shouldBe true // on by default when available

            viewModel.onAction(CreatePinAction.PinChanged("123456"))
            viewModel.onAction(CreatePinAction.ConfirmPinChanged("123456"))
            viewModel.onAction(CreatePinAction.BiometricToggled(false))
            viewModel.onAction(CreatePinAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().navigation shouldBe CreatePinNavigation.PinCreated
        }
        savedPin shouldBe "123456"
        savedBiometric shouldBe false
    }

    test("biometrics can't be enabled when the device has none") {
        val viewModel =
            CreatePinViewModel(createPin = { _, _ -> AppResult.Success(Unit) }, isBiometricAvailable = { false })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.onAction(CreatePinAction.BiometricToggled(true))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().isBiometricEnabled shouldBe false
        }
    }
})
