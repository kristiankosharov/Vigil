package app.vigil.feature.applock.ui.create

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.mvi.UiNavigation
import app.vigil.common.ui.mvi.UiState
import app.vigil.feature.applock.ui.biometric.BiometricRequest

/**
 * Onboarding after sign-in (docs/design/README.md, 1.1–1.3): create the PIN, confirm it, then offer
 * fingerprint unlock. [Biometric] is skipped on devices without strong biometrics.
 */
internal enum class CreatePinStep { Enter, Confirm, Biometric }

/** Whether the confirmation matches; known only once all its digits are entered. */
internal enum class PinMatch { Unknown, Match, Mismatch }

@Immutable
internal data class CreatePinUiState(
    val step: CreatePinStep,
    /** 3 with the fingerprint step, 2 without it. */
    val stepCount: Int,
    /** How many digits the current step has (the PIN on Enter, the confirmation on Confirm). */
    val inputLength: Int,
    val match: PinMatch,
    val isSaving: Boolean,
    val biometricRequest: BiometricRequest?,
    val effect: UiEffect<CreatePinAction>?,
    val navigation: CreatePinNavigation?,
) : UiState

internal sealed interface CreatePinAction : UiAction {
    data class Digit(val digit: Int) : CreatePinAction

    data object Delete : CreatePinAction

    /** Enter → Confirm → Biometric (or save, without biometrics). */
    data object Continue : CreatePinAction

    /** One step back; on Enter the screen leaves onboarding instead. */
    data object Back : CreatePinAction

    /** "Enable fingerprint": confirm a fingerprint first, then save with biometrics on. */
    data object EnableBiometric : CreatePinAction

    data object BiometricSucceeded : CreatePinAction

    class BiometricRequestConsumed(val request: BiometricRequest) : CreatePinAction

    /** "Not now": save with biometrics off. */
    data object SkipBiometric : CreatePinAction

    class EffectConsumed(val effect: UiEffect<CreatePinAction>) : CreatePinAction

    data class NavigationConsumed(val navigation: CreatePinNavigation) : CreatePinAction
}

internal sealed interface CreatePinNavigation : UiNavigation {
    data object PinCreated : CreatePinNavigation
}
