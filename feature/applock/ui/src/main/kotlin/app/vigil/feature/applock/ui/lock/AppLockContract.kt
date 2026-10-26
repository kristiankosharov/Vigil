package app.vigil.feature.applock.ui.lock

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.mvi.UiState
import app.vigil.feature.applock.ui.biometric.BiometricRequest

internal enum class AppLockStatus {
    /** Not known yet: the app stays covered (fail closed). */
    Loading,
    Locked,
    Unlocked,
}

@Immutable
internal data class AppLockUiState(
    val status: AppLockStatus,
    /** How many digits are typed; the PIN itself never leaves the ViewModel. */
    val pinLength: Int,
    val isPinWrong: Boolean,
    /** Attempts left before everything is erased, once a wrong PIN told us; null until then. */
    val attemptsLeft: Int?,
    /** Seconds until PIN entry opens again after too many wrong PINs; 0 when it is open. */
    val retrySeconds: Int,
    val isChecking: Boolean,
    val isBiometricEnabled: Boolean,
    val biometricRequest: BiometricRequest?,
    val effect: UiEffect<AppLockAction>?,
) : UiState

internal sealed interface AppLockAction : UiAction {
    /** The PIN is checked as soon as it has all its digits. */
    data class Digit(val digit: Int) : AppLockAction

    data object Delete : AppLockAction

    data object UseBiometric : AppLockAction

    data object BiometricSucceeded : AppLockAction

    class BiometricRequestConsumed(val request: BiometricRequest) : AppLockAction

    data object AppBackgrounded : AppLockAction

    data object AppForegrounded : AppLockAction

    data object Retry : AppLockAction

    class EffectConsumed(val effect: UiEffect<AppLockAction>) : AppLockAction
}
