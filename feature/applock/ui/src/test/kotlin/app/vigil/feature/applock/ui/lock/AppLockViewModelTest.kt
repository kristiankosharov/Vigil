package app.vigil.feature.applock.ui.lock

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.feature.applock.domain.model.AppLockState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class AppLockViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    /** A fake app lock: correct PIN "123456"; unlocking flips the state flow like the real session. */
    class FakeLock(locked: Boolean, biometric: Boolean = false) {
        val state = MutableStateFlow<AppResult<AppLockState>>(AppResult.Success(AppLockState(locked, biometric)))
        var backgrounded = 0
        var foregrounded = 0

        fun unlock() {
            state.value = AppResult.Success(AppLockState(isLocked = false, isBiometricEnabled = false))
        }

        fun viewModel() = AppLockViewModel(
            observeLockState = { state },
            unlockWithPin = { pin -> AppResult.Success(pin == "123456").also { if (pin == "123456") unlock() } },
            unlockWithBiometric = { unlock() },
            markAppBackgrounded = { backgrounded++ },
            markAppForegrounded = { foregrounded++ },
        )
    }

    test("covered while loading, then unlocked when no lock is needed") {
        val viewModel = FakeLock(locked = false).viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().status shouldBe AppLockStatus.Loading
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().status shouldBe AppLockStatus.Unlocked
        }
    }

    test("a full wrong PIN is checked at once, shows the error and clears the field; the right PIN unlocks") {
        val viewModel = FakeLock(locked = true).viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().status shouldBe AppLockStatus.Locked

            viewModel.onAction(AppLockAction.PinChanged("000000"))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.isPinWrong shouldBe true
                it.pin shouldBe ""
                it.status shouldBe AppLockStatus.Locked
            }

            viewModel.onAction(AppLockAction.PinChanged("123456"))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().status shouldBe AppLockStatus.Unlocked
        }
    }

    test("the PIN field takes only digits, up to the PIN length, and isn't checked before") {
        val viewModel = FakeLock(locked = true).viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle() // locked; locking clears the field
            viewModel.onAction(AppLockAction.PinChanged("12a"))
            viewModel.onAction(AppLockAction.PinChanged("1234567"))
            viewModel.onAction(AppLockAction.PinChanged("1234"))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.pin shouldBe "1234"
                it.isPinWrong shouldBe false
            }
        }
    }

    test("with biometrics enabled the prompt is requested when locked; success unlocks") {
        val viewModel = FakeLock(locked = true, biometric = true).viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            val request = expectMostRecentItem().biometricRequest.shouldNotBeNull()

            viewModel.onAction(AppLockAction.BiometricRequestConsumed(request))
            viewModel.onAction(AppLockAction.BiometricSucceeded)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.biometricRequest shouldBe null
                it.status shouldBe AppLockStatus.Unlocked
            }
        }
    }

    test("if the lock state can't be read the app stays covered and offers Retry") {
        val viewModel = AppLockViewModel(
            observeLockState = { flowOf(AppResult.Failure(AppError.Local.Storage(null))) },
            unlockWithPin = { AppResult.Success(false) },
            unlockWithBiometric = {},
            markAppBackgrounded = {},
            markAppForegrounded = {},
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.status shouldBe AppLockStatus.Locked
            state.effect.shouldBeInstanceOf<UiEffect.ShowSnackbar<AppLockAction>>().action shouldBe AppLockAction.Retry
        }
    }

    test("background and foreground are forwarded") {
        val lock = FakeLock(locked = false)
        val viewModel = lock.viewModel()

        viewModel.onAction(AppLockAction.AppBackgrounded)
        viewModel.onAction(AppLockAction.AppForegrounded)

        lock.backgrounded shouldBe 1
        lock.foregrounded shouldBe 1
    }
})
