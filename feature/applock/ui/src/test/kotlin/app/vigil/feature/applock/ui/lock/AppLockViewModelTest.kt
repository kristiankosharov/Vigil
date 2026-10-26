package app.vigil.feature.applock.ui.lock

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.feature.applock.domain.model.AppLockState
import app.vigil.feature.applock.domain.model.BiometricCipher
import app.vigil.feature.applock.domain.model.PinCheck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import javax.crypto.Cipher
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class AppLockViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    /**
     * A fake app lock: correct PIN "123456"; a wrong one leaves [attemptsLeft] and waits [wrongDelay].
     * Unlocking flips the state flow like the real session.
     */
    class FakeLock(locked: Boolean, biometric: Boolean = false, val wrongDelay: Duration = Duration.ZERO) {
        val state = MutableStateFlow<AppResult<AppLockState>>(AppResult.Success(AppLockState(locked, biometric)))
        val checked = mutableListOf<String>()
        val cipher = BiometricCipher(Cipher.getInstance("AES/GCM/NoPadding"))
        var unlockedWith: BiometricCipher? = null
        var backgrounded = 0
        var foregrounded = 0

        fun unlock() {
            state.value = AppResult.Success(AppLockState(isLocked = false, isBiometricEnabled = false))
        }

        fun viewModel() = AppLockViewModel(
            observeLockState = { state },
            unlockWithPin = { pin ->
                val text = String(pin).also(checked::add)
                if (text == "123456") {
                    unlock()
                    AppResult.Success(PinCheck.Correct)
                } else {
                    AppResult.Success(PinCheck.Wrong(attemptsLeft = 3, retryAfter = wrongDelay))
                }
            },
            prepareBiometricUnlock = { AppResult.Success(cipher) },
            unlockWithBiometric = { cipher ->
                unlockedWith = cipher
                unlock()
                AppResult.Success(Unit)
            },
            markAppBackgrounded = { backgrounded++ },
            markAppForegrounded = { foregrounded++ },
        )
    }

    fun AppLockViewModel.type(digits: String) = digits.forEach { onAction(AppLockAction.Digit(it.digitToInt())) }

    test("covered while loading, then unlocked when no lock is needed") {
        val viewModel = FakeLock(locked = false).viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().status shouldBe AppLockStatus.Loading
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().status shouldBe AppLockStatus.Unlocked
        }
    }

    test("a full wrong PIN is checked at once, shows the error with attempts left and clears; the right PIN unlocks") {
        val lock = FakeLock(locked = true)
        val viewModel = lock.viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().status shouldBe AppLockStatus.Locked

            viewModel.type("000000")
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.isPinWrong shouldBe true
                it.attemptsLeft shouldBe 3
                it.pinLength shouldBe 0
                it.status shouldBe AppLockStatus.Locked
            }

            viewModel.type("123456")
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().status shouldBe AppLockStatus.Unlocked
            lock.checked shouldBe listOf("000000", "123456")
        }
    }

    test("the PIN is checked only once it has 6 digits; extra digits are ignored") {
        val lock = FakeLock(locked = true)
        val viewModel = lock.viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            viewModel.type("1234")
            viewModel.onAction(AppLockAction.Delete)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.pinLength shouldBe 3
                it.isPinWrong shouldBe false
            }
            lock.checked shouldBe emptyList()
        }
    }

    test("after too many wrong PINs the keypad waits for the countdown") {
        val lock = FakeLock(locked = true, wrongDelay = 30.seconds)
        val viewModel = lock.viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            viewModel.type("000000")
            mainDispatcher.scheduler.runCurrent()
            expectMostRecentItem().retrySeconds shouldBe 30

            viewModel.type("1") // ignored while waiting
            mainDispatcher.scheduler.advanceTimeBy(10.seconds)
            mainDispatcher.scheduler.runCurrent()
            expectMostRecentItem().let {
                it.retrySeconds shouldBe 20
                it.pinLength shouldBe 0
            }

            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().retrySeconds shouldBe 0
        }
    }

    test("a lockout still running when the app opens blocks the keypad") {
        val lock = FakeLock(locked = true)
        lock.state.value =
            AppResult.Success(AppLockState(isLocked = true, isBiometricEnabled = false, retryAfter = 5.seconds))
        val viewModel = lock.viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.runCurrent()
            expectMostRecentItem().retrySeconds shouldBe 5
        }
    }

    test("with biometrics enabled the prompt is requested with its cipher; success unlocks with that cipher") {
        val lock = FakeLock(locked = true, biometric = true)
        val viewModel = lock.viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            val request = expectMostRecentItem().biometricRequest.shouldNotBeNull()
            request.cipher shouldBe lock.cipher

            viewModel.onAction(AppLockAction.BiometricRequestConsumed(request))
            viewModel.onAction(AppLockAction.BiometricSucceeded)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.biometricRequest shouldBe null
                it.status shouldBe AppLockStatus.Unlocked
            }
            lock.unlockedWith shouldBe lock.cipher
        }
    }

    test("if the lock state can't be read the app stays covered and offers Retry") {
        val viewModel = AppLockViewModel(
            observeLockState = { flowOf(AppResult.Failure(AppError.Local.Storage(null))) },
            unlockWithPin = { AppResult.Success(PinCheck.Correct) },
            prepareBiometricUnlock = { AppResult.Success(null) },
            unlockWithBiometric = { AppResult.Success(Unit) },
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

    test("background clears a half-typed PIN; background and foreground are forwarded") {
        val lock = FakeLock(locked = true)
        val viewModel = lock.viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            viewModel.type("12")
            viewModel.onAction(AppLockAction.AppBackgrounded)
            viewModel.onAction(AppLockAction.AppForegrounded)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().pinLength shouldBe 0
        }
        lock.backgrounded shouldBe 1
        lock.foregrounded shouldBe 1
    }
})
