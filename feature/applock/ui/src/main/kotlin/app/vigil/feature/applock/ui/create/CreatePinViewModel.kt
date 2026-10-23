package app.vigil.feature.applock.ui.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import app.vigil.common.core.onFailure
import app.vigil.common.core.onSuccess
import app.vigil.common.ui.mvi.BaseViewModel
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.text.UiText
import app.vigil.common.ui.text.toUiText
import app.vigil.feature.applock.domain.model.PinRules
import app.vigil.feature.applock.domain.usecase.CreatePinUseCase
import app.vigil.feature.applock.domain.usecase.IsBiometricAvailableUseCase
import app.vigil.feature.applock.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
internal class CreatePinViewModel @Inject constructor(
    private val createPin: CreatePinUseCase,
    isBiometricAvailable: IsBiometricAvailableUseCase,
) : BaseViewModel<CreatePinUiState, CreatePinAction, CreatePinNavigation>() {

    // Never savedStateHandle: the PIN must not be written to the saved instance state.
    private var pin by mutableStateOf("")
    private var confirmPin by mutableStateOf("")
    private val isBiometricAvailable = isBiometricAvailable()
    private var isBiometricEnabled by mutableStateOf(this.isBiometricAvailable)
    private var error by mutableStateOf<UiText?>(null)
    private var isSaving by mutableStateOf(false)
    private var saveJob: Job? = null

    @Composable
    override fun uiState() = CreatePinUiState(
        pin = pin,
        confirmPin = confirmPin,
        isBiometricAvailable = isBiometricAvailable,
        isBiometricEnabled = isBiometricEnabled,
        error = error,
        isSaving = isSaving,
        effect = currentEffect,
        navigation = currentNavigation,
    )

    override fun onAction(action: CreatePinAction) {
        when (action) {
            is CreatePinAction.PinChanged -> action.pin.ifPinInput { pin = it }
            is CreatePinAction.ConfirmPinChanged -> action.pin.ifPinInput { confirmPin = it }
            is CreatePinAction.BiometricToggled -> isBiometricEnabled = action.enabled && isBiometricAvailable
            CreatePinAction.Submit -> submit()
            is CreatePinAction.EffectConsumed -> consumeEffect(action.effect)
            is CreatePinAction.NavigationConsumed -> consumeNavigation(action.navigation)
        }
    }

    private inline fun String.ifPinInput(update: (String) -> Unit) {
        if (length <= PinRules.MAX_LENGTH && all(Char::isDigit)) {
            update(this)
            error = null
        }
    }

    private fun submit() {
        if (saveJob?.isActive == true) return
        error = when {
            !PinRules.isValid(pin) ->
                UiText.Plural(
                    id = R.plurals.applock_error_pin_format,
                    count = PinRules.MAX_LENGTH,
                    args = listOf(PinRules.MIN_LENGTH, PinRules.MAX_LENGTH),
                )

            pin != confirmPin -> UiText.Resource(R.string.applock_error_pin_mismatch)

            else -> null
        }
        if (error != null) return
        saveJob = viewModelScope.launch {
            isSaving = true
            try {
                createPin(pin, isBiometricEnabled)
                    .onSuccess { navigate(CreatePinNavigation.PinCreated) }
                    .onFailure { appError -> sendEffect(UiEffect.ShowSnackbar(appError.toUiText())) }
            } finally {
                isSaving = false
            }
        }
    }
}
