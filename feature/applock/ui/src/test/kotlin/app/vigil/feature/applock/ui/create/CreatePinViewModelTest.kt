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

    fun viewModel(
        biometricAvailable: Boolean = false,
        createPin: suspend (String, Boolean) -> AppResult<Unit> = { _, _ -> AppResult.Success(Unit) },
    ) = CreatePinViewModel(
        createPin = { pin, biometric -> createPin(pin, biometric) },
        isBiometricAvailable = { biometricAvailable },
    )

    fun CreatePinViewModel.type(pin: String) = onAction(CreatePinAction.InputChanged(pin))

    test("Next with a too short PIN stays on Enter with the format error") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.type("12")
            viewModel.onAction(CreatePinAction.Next)
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.step shouldBe CreatePinStep.Enter
            state.error shouldBe UiText.Plural(R.plurals.applock_error_pin_format, 8, listOf(4, 8))
        }
    }

    test("a valid PIN moves to Repeat with an empty input") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.type("1234")
            viewModel.onAction(CreatePinAction.Next)
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.step shouldBe CreatePinStep.Repeat
            state.input shouldBe ""
        }
    }

    test("a repetition that doesn't match starts again at Enter with the mismatch error") {
        var saved = 0
        val viewModel = viewModel(createPin = { _, _ -> AppResult.Success(Unit).also { saved++ } })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.type("1234")
            viewModel.onAction(CreatePinAction.Next)
            viewModel.type("1235")
            viewModel.onAction(CreatePinAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.step shouldBe CreatePinStep.Enter
            state.input shouldBe ""
            state.error shouldBe UiText.Resource(R.string.applock_error_pin_mismatch)
        }
        saved shouldBe 0
    }

    test("Back on Repeat returns to Enter and clears both PINs") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.type("1234")
            viewModel.onAction(CreatePinAction.Next)
            viewModel.onAction(CreatePinAction.Back)
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.step shouldBe CreatePinStep.Enter
            state.input shouldBe ""
        }
    }

    test("a matching repetition saves the PIN with the biometric choice, then navigates") {
        var savedPin: String? = null
        var savedBiometric: Boolean? = null
        val viewModel = viewModel(
            biometricAvailable = true,
            createPin = { pin, biometric ->
                savedPin = pin
                savedBiometric = biometric
                AppResult.Success(Unit)
            },
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().isBiometricEnabled shouldBe true // on by default when available

            viewModel.type("123456")
            viewModel.onAction(CreatePinAction.Next)
            viewModel.type("123456")
            viewModel.onAction(CreatePinAction.BiometricToggled(false))
            viewModel.onAction(CreatePinAction.Submit)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().navigation shouldBe CreatePinNavigation.PinCreated
        }
        savedPin shouldBe "123456"
        savedBiometric shouldBe false
    }

    test("input takes only digits, up to the maximum length") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.type("12a")
            viewModel.type("123456789")
            viewModel.type("1234")
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().input shouldBe "1234"
        }
    }

    test("biometrics can't be enabled when the device has none") {
        val viewModel = viewModel(biometricAvailable = false)

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.onAction(CreatePinAction.BiometricToggled(true))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().isBiometricEnabled shouldBe false
        }
    }
})
