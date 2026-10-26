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
import app.vigil.common.ui.text.toUiText
import app.vigil.feature.applock.domain.model.PinRules
import app.vigil.feature.applock.domain.usecase.CreatePinUseCase
import app.vigil.feature.applock.domain.usecase.IsBiometricAvailableUseCase
import app.vigil.feature.applock.ui.biometric.BiometricRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
internal class CreatePinViewModel @Inject constructor(
    private val createPin: CreatePinUseCase,
    isBiometricAvailable: IsBiometricAvailableUseCase,
) : BaseViewModel<CreatePinUiState, CreatePinAction, CreatePinNavigation>() {

    private val isBiometricAvailable = isBiometricAvailable()
    private var step by mutableStateOf(CreatePinStep.Enter)

    // Never savedStateHandle: the PIN must not be written to the saved instance state.
    private var pin by mutableStateOf("")
    private var confirmation by mutableStateOf("")
    private var isSaving by mutableStateOf(false)
    private var biometricRequest by mutableStateOf<BiometricRequest?>(null)
    private var saveJob: Job? = null

    @Composable
    override fun uiState() = CreatePinUiState(
        step = step,
        stepCount = if (isBiometricAvailable) STEPS_WITH_BIOMETRIC else STEPS_WITHOUT_BIOMETRIC,
        inputLength = if (step == CreatePinStep.Enter) pin.length else confirmation.length,
        match = when {
            confirmation.length < PinRules.LENGTH -> PinMatch.Unknown
            confirmation == pin -> PinMatch.Match
            else -> PinMatch.Mismatch
        },
        isSaving = isSaving,
        biometricRequest = biometricRequest,
        effect = currentEffect,
        navigation = currentNavigation,
    )

    override fun onAction(action: CreatePinAction) {
        when (action) {
            is CreatePinAction.Digit -> addDigit(action.digit)

            CreatePinAction.Delete -> deleteDigit()

            CreatePinAction.Continue -> next()

            CreatePinAction.Back -> back()

            CreatePinAction.EnableBiometric -> if (step ==
                CreatePinStep.Biometric
            ) {
                biometricRequest = BiometricRequest()
            }

            CreatePinAction.BiometricSucceeded -> save(biometricEnabled = true)

            is CreatePinAction.BiometricRequestConsumed ->
                if (biometricRequest === action.request) biometricRequest = null

            CreatePinAction.SkipBiometric -> save(biometricEnabled = false)

            is CreatePinAction.EffectConsumed -> consumeEffect(action.effect)

            is CreatePinAction.NavigationConsumed -> consumeNavigation(action.navigation)
        }
    }

    private fun addDigit(digit: Int) {
        when (step) {
            CreatePinStep.Enter -> if (pin.length < PinRules.LENGTH) pin += digit

            // After a mismatch the next digit starts a new attempt.
            CreatePinStep.Confirm -> confirmation = when {
                confirmation.length < PinRules.LENGTH -> confirmation + digit
                confirmation != pin -> digit.toString()
                else -> confirmation
            }

            CreatePinStep.Biometric -> Unit
        }
    }

    private fun deleteDigit() {
        when (step) {
            CreatePinStep.Enter -> pin = pin.dropLast(1)
            CreatePinStep.Confirm -> confirmation = confirmation.dropLast(1)
            CreatePinStep.Biometric -> Unit
        }
    }

    private fun next() {
        when (step) {
            CreatePinStep.Enter -> if (PinRules.isValid(pin)) {
                confirmation = ""
                step = CreatePinStep.Confirm
            }

            CreatePinStep.Confirm -> if (confirmation == pin && PinRules.isValid(pin)) {
                if (isBiometricAvailable) step = CreatePinStep.Biometric else save(biometricEnabled = false)
            }

            CreatePinStep.Biometric -> Unit
        }
    }

    private fun back() {
        if (isSaving) return
        when (step) {
            CreatePinStep.Enter -> Unit

            // the screen leaves onboarding
            CreatePinStep.Confirm -> {
                pin = ""
                confirmation = ""
                step = CreatePinStep.Enter
            }

            CreatePinStep.Biometric -> {
                confirmation = ""
                step = CreatePinStep.Confirm
            }
        }
    }

    private fun save(biometricEnabled: Boolean) {
        if (saveJob?.isActive == true || confirmation != pin || !PinRules.isValid(pin)) return
        saveJob = viewModelScope.launch {
            isSaving = true
            try {
                createPin(pin, biometricEnabled && isBiometricAvailable)
                    .onSuccess { navigate(CreatePinNavigation.PinCreated) }
                    .onFailure { appError -> sendEffect(UiEffect.ShowSnackbar(appError.toUiText())) }
            } finally {
                isSaving = false
            }
        }
    }

    private companion object {
        const val STEPS_WITH_BIOMETRIC = 3
        const val STEPS_WITHOUT_BIOMETRIC = 2
    }
}
