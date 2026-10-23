package app.vigil.feature.applock.ui.create

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.mvi.UiNavigation
import app.vigil.common.ui.mvi.UiState
import app.vigil.common.ui.text.UiText

/** Enter a PIN, then repeat it; a mismatch starts again at [Enter]. */
internal enum class CreatePinStep { Enter, Repeat }

@Immutable
internal data class CreatePinUiState(
    val step: CreatePinStep,
    /** The digits of the current step (the PIN on Enter, the repetition on Repeat). */
    val input: String,
    val isBiometricAvailable: Boolean,
    val isBiometricEnabled: Boolean,
    val error: UiText?,
    val isSaving: Boolean,
    val effect: UiEffect<CreatePinAction>?,
    val navigation: CreatePinNavigation?,
) : UiState

internal sealed interface CreatePinAction : UiAction {
    /** The current step's digits changed (number pad). */
    data class InputChanged(val input: String) : CreatePinAction

    /** Enter → Repeat, once the PIN is valid. */
    data object Next : CreatePinAction

    /** Repeat → Enter (system Back on the repeat step). */
    data object Back : CreatePinAction

    data class BiometricToggled(val enabled: Boolean) : CreatePinAction

    /** On Repeat: save if both match, otherwise start again. */
    data object Submit : CreatePinAction

    class EffectConsumed(val effect: UiEffect<CreatePinAction>) : CreatePinAction

    data class NavigationConsumed(val navigation: CreatePinNavigation) : CreatePinAction
}

internal sealed interface CreatePinNavigation : UiNavigation {
    data object PinCreated : CreatePinNavigation
}
