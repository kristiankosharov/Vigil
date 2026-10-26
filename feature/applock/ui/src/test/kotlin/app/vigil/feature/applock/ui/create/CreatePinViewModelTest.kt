package app.vigil.feature.applock.ui.create

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.feature.applock.domain.model.BiometricCipher
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import javax.crypto.Cipher

class CreatePinViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    class Saved {
        var pin: String? = null
        var biometric: Boolean? = null
        var count = 0
    }

    fun viewModel(
        saved: Saved = Saved(),
        biometricAvailable: Boolean = true,
        result: AppResult<Unit> = AppResult.Success(Unit),
    ) = CreatePinViewModel(
        createPin = { pin, biometric ->
            saved.pin = String(pin)
            saved.biometric = biometric != null
            saved.count++
            result
        },
        prepareBiometricEnrollment = { AppResult.Success(BiometricCipher(Cipher.getInstance("AES/GCM/NoPadding"))) },
        isBiometricAvailable = { biometricAvailable },
    )

    fun CreatePinViewModel.type(digits: String) = digits.forEach { onAction(CreatePinAction.Digit(it.digitToInt())) }

    fun CreatePinViewModel.createAndConfirm(pin: String = "482916") {
        type(pin)
        onAction(CreatePinAction.Continue)
        type(pin)
        onAction(CreatePinAction.Continue)
    }

    test("Continue needs all 6 digits; more digits are ignored") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.type("48291")
            viewModel.onAction(CreatePinAction.Continue)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().step shouldBe CreatePinStep.Enter

            viewModel.type("67")
            viewModel.onAction(CreatePinAction.Continue)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.step shouldBe CreatePinStep.Confirm
                it.inputLength shouldBe 0
                it.match shouldBe PinMatch.Unknown
            }
        }
    }

    test("an easy PIN is refused on Continue and cleared; typing again clears the message") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.type("123456")
            viewModel.onAction(CreatePinAction.Continue)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.step shouldBe CreatePinStep.Enter
                it.isPinWeak shouldBe true
                it.inputLength shouldBe 0
            }

            viewModel.type("4")
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().isPinWeak shouldBe false
        }
    }

    test("the confirmation reports a match or a mismatch once complete") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.type("482916")
            viewModel.onAction(CreatePinAction.Continue)
            viewModel.type("482910")
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().match shouldBe PinMatch.Mismatch

            viewModel.onAction(CreatePinAction.Continue) // not allowed on a mismatch
            viewModel.type("4") // a new attempt starts with this digit
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.step shouldBe CreatePinStep.Confirm
                it.inputLength shouldBe 1
                it.match shouldBe PinMatch.Unknown
            }

            viewModel.type("82916")
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().match shouldBe PinMatch.Match
        }
    }

    test("Back goes one step back and clears what that step entered") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.createAndConfirm()
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().step shouldBe CreatePinStep.Biometric

            viewModel.onAction(CreatePinAction.Back)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.step shouldBe CreatePinStep.Confirm
                it.inputLength shouldBe 0
            }

            viewModel.onAction(CreatePinAction.Back)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.step shouldBe CreatePinStep.Enter
                it.inputLength shouldBe 0
            }
        }
    }

    test("Enable fingerprint asks for a fingerprint, then saves with biometrics on and navigates") {
        val saved = Saved()
        val viewModel = viewModel(saved)

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.createAndConfirm()
            viewModel.onAction(CreatePinAction.EnableBiometric)
            mainDispatcher.scheduler.advanceUntilIdle()
            val request = expectMostRecentItem().biometricRequest.shouldNotBeNull()
            saved.count shouldBe 0 // nothing saved before the fingerprint is confirmed

            viewModel.onAction(CreatePinAction.BiometricRequestConsumed(request))
            viewModel.onAction(CreatePinAction.BiometricSucceeded)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().navigation shouldBe CreatePinNavigation.PinCreated
        }
        saved.pin shouldBe "482916"
        saved.biometric shouldBe true
    }

    test("Not now saves with biometrics off") {
        val saved = Saved()
        val viewModel = viewModel(saved)

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.createAndConfirm()
            viewModel.onAction(CreatePinAction.SkipBiometric)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().navigation shouldBe CreatePinNavigation.PinCreated
        }
        saved.biometric shouldBe false
    }

    test("without biometrics there are 2 steps and Continue on Confirm saves") {
        val saved = Saved()
        val viewModel = viewModel(saved, biometricAvailable = false)

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().stepCount shouldBe 2
            viewModel.createAndConfirm()
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().navigation shouldBe CreatePinNavigation.PinCreated
        }
        saved.biometric shouldBe false
    }

    test("a save failure shows a snackbar and stays on the step") {
        val viewModel = viewModel(result = AppResult.Failure(AppError.Local.Storage(null)))

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.createAndConfirm()
            viewModel.onAction(CreatePinAction.SkipBiometric)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.effect.shouldBeInstanceOf<UiEffect.ShowSnackbar<CreatePinAction>>()
                it.navigation shouldBe null
                it.step shouldBe CreatePinStep.Biometric
                it.isSaving shouldBe false
            }
        }
    }
})
