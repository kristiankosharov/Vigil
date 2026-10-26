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
    val pin: String,
    val isPinWrong: Boolean,
    val isChecking: Boolean,
    val isBiometricEnabled: Boolean,
    val biometricRequest: BiometricRequest?,
    val effect: UiEffect<AppLockAction>?,
) : UiState

internal sealed interface AppLockAction : UiAction {
    /** The PIN is checked as soon as it has all its digits. */
    data class PinChanged(val pin: String) : AppLockAction

    data object UseBiometric : AppLockAction

    data object BiometricSucceeded : AppLockAction

    class BiometricRequestConsumed(val request: BiometricRequest) : AppLockAction

    data object AppBackgrounded : AppLockAction

    data object AppForegrounded : AppLockAction

    data object Retry : AppLockAction

    class EffectConsumed(val effect: UiEffect<AppLockAction>) : AppLockAction
}
