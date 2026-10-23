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

    private var step by mutableStateOf(CreatePinStep.Enter)

    // Never savedStateHandle: the PIN must not be written to the saved instance state.
    private var pin by mutableStateOf("")
    private var repeatedPin by mutableStateOf("")
    private val isBiometricAvailable = isBiometricAvailable()
    private var isBiometricEnabled by mutableStateOf(this.isBiometricAvailable)
    private var error by mutableStateOf<UiText?>(null)
    private var isSaving by mutableStateOf(false)
    private var saveJob: Job? = null

    @Composable
    override fun uiState() = CreatePinUiState(
        step = step,
        input = if (step == CreatePinStep.Enter) pin else repeatedPin,
        isBiometricAvailable = isBiometricAvailable,
        isBiometricEnabled = isBiometricEnabled,
        error = error,
        isSaving = isSaving,
        effect = currentEffect,
        navigation = currentNavigation,
    )

    override fun onAction(action: CreatePinAction) {
        when (action) {
            is CreatePinAction.InputChanged -> changeInput(action.input)
            CreatePinAction.Next -> next()
            CreatePinAction.Back -> startAgain(error = null)
            is CreatePinAction.BiometricToggled -> isBiometricEnabled = action.enabled && isBiometricAvailable
            CreatePinAction.Submit -> submit()
            is CreatePinAction.EffectConsumed -> consumeEffect(action.effect)
            is CreatePinAction.NavigationConsumed -> consumeNavigation(action.navigation)
        }
    }

    private fun changeInput(input: String) {
        if (input.length > PinRules.MAX_LENGTH || !input.all(Char::isDigit)) return
        if (step == CreatePinStep.Enter) pin = input else repeatedPin = input
        error = null
    }

    private fun next() {
        if (!PinRules.isValid(pin)) {
            error = UiText.Plural(
                id = R.plurals.applock_error_pin_format,
                count = PinRules.MAX_LENGTH,
                args = listOf(PinRules.MIN_LENGTH, PinRules.MAX_LENGTH),
            )
            return
        }
        repeatedPin = ""
        error = null
        step = CreatePinStep.Repeat
    }

    private fun startAgain(error: UiText?) {
        step = CreatePinStep.Enter
        pin = ""
        repeatedPin = ""
        this.error = error
    }

    private fun submit() {
        if (step != CreatePinStep.Repeat || saveJob?.isActive == true) return
        if (repeatedPin != pin) {
            startAgain(error = UiText.Resource(R.string.applock_error_pin_mismatch))
            return
        }
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
