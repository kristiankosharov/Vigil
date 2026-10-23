package app.vigil.feature.applock.ui.create

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.mvi.UiNavigation
import app.vigil.common.ui.mvi.UiState
import app.vigil.common.ui.text.UiText

@Immutable
internal data class CreatePinUiState(
    val pin: String,
    val confirmPin: String,
    val isBiometricAvailable: Boolean,
    val isBiometricEnabled: Boolean,
    val error: UiText?,
    val isSaving: Boolean,
    val effect: UiEffect<CreatePinAction>?,
    val navigation: CreatePinNavigation?,
) : UiState

internal sealed interface CreatePinAction : UiAction {
    data class PinChanged(val pin: String) : CreatePinAction

    data class ConfirmPinChanged(val pin: String) : CreatePinAction

    data class BiometricToggled(val enabled: Boolean) : CreatePinAction

    data object Submit : CreatePinAction

    class EffectConsumed(val effect: UiEffect<CreatePinAction>) : CreatePinAction

    data class NavigationConsumed(val navigation: CreatePinNavigation) : CreatePinAction
}

internal sealed interface CreatePinNavigation : UiNavigation {
    data object PinCreated : CreatePinNavigation
}
