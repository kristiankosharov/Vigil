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
import app.vigil.feature.applock.domain.model.BiometricCipher
import app.vigil.feature.applock.domain.model.PinRules
import app.vigil.feature.applock.domain.usecase.CreatePinUseCase
import app.vigil.feature.applock.domain.usecase.IsBiometricAvailableUseCase
import app.vigil.feature.applock.domain.usecase.PrepareBiometricEnrollmentUseCase
import app.vigil.feature.applock.ui.biometric.BiometricRequest
import app.vigil.feature.applock.ui.components.PinBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
internal class CreatePinViewModel @Inject constructor(
    private val createPin: CreatePinUseCase,
    private val prepareBiometricEnrollment: PrepareBiometricEnrollmentUseCase,
    isBiometricAvailable: IsBiometricAvailableUseCase,
) : BaseViewModel<CreatePinUiState, CreatePinAction, CreatePinNavigation>() {

    private val isBiometricAvailable = isBiometricAvailable()
    private var step by mutableStateOf(CreatePinStep.Enter)

    // Never savedStateHandle: the PIN must not be written to the saved instance state.
    private val pin = PinBuffer()
    private val confirmation = PinBuffer()
    private var isPinWeak by mutableStateOf(false)
    private var isSaving by mutableStateOf(false)
    private var biometricRequest by mutableStateOf<BiometricRequest?>(null)

    /** The cipher of the fingerprint prompt shown; it wraps the vault key once the prompt succeeds. */
    private var promptedCipher: BiometricCipher? = null
    private var saveJob: Job? = null

    @Composable
    override fun uiState() = CreatePinUiState(
        step = step,
        stepCount = if (isBiometricAvailable) STEPS_WITH_BIOMETRIC else STEPS_WITHOUT_BIOMETRIC,
        inputLength = if (step == CreatePinStep.Enter) pin.length else confirmation.length,
        match = when {
            !confirmation.isFull -> PinMatch.Unknown
            confirmation.contentEquals(pin) -> PinMatch.Match
            else -> PinMatch.Mismatch
        },
        isPinWeak = isPinWeak,
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

            CreatePinAction.EnableBiometric -> if (step == CreatePinStep.Biometric) requestBiometric()

            CreatePinAction.BiometricSucceeded -> promptedCipher?.let { save(biometric = it) }

            is CreatePinAction.BiometricRequestConsumed -> if (biometricRequest === action.request) {
                promptedCipher = action.request.cipher
                biometricRequest = null
            }

            CreatePinAction.SkipBiometric -> save(biometric = null)

            is CreatePinAction.EffectConsumed -> consumeEffect(action.effect)

            is CreatePinAction.NavigationConsumed -> consumeNavigation(action.navigation)
        }
    }

    override fun onCleared() {
        pin.clear()
        confirmation.clear()
    }

    private fun addDigit(digit: Int) {
        when (step) {
            CreatePinStep.Enter -> {
                isPinWeak = false
                pin.add(digit)
            }

            // After a mismatch the next digit starts a new attempt.
            CreatePinStep.Confirm -> {
                if (confirmation.isFull && !confirmation.contentEquals(pin)) confirmation.clear()
                confirmation.add(digit)
            }

            CreatePinStep.Biometric -> Unit
        }
    }

    private fun deleteDigit() {
        when (step) {
            CreatePinStep.Enter -> pin.delete()
            CreatePinStep.Confirm -> confirmation.delete()
            CreatePinStep.Biometric -> Unit
        }
    }

    private fun next() {
        when (step) {
            CreatePinStep.Enter -> if (pin.isFull) {
                val digits = pin.copy()
                val weak = PinRules.isWeak(digits)
                digits.fill(Char.MIN_VALUE)
                if (weak) {
                    isPinWeak = true
                    pin.clear()
                } else {
                    confirmation.clear()
                    step = CreatePinStep.Confirm
                }
            }

            CreatePinStep.Confirm -> if (confirmation.isFull && confirmation.contentEquals(pin)) {
                if (isBiometricAvailable) step = CreatePinStep.Biometric else save(biometric = null)
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
                pin.clear()
                confirmation.clear()
                step = CreatePinStep.Enter
            }

            CreatePinStep.Biometric -> {
                confirmation.clear()
                step = CreatePinStep.Confirm
            }
        }
    }

    private fun requestBiometric() {
        prepareBiometricEnrollment()
            .onSuccess { biometricRequest = BiometricRequest(it) }
            .onFailure { appError -> sendEffect(UiEffect.ShowSnackbar(appError.toUiText())) }
    }

    private fun save(biometric: BiometricCipher?) {
        if (saveJob?.isActive == true || !pin.isFull || !confirmation.contentEquals(pin)) return
        promptedCipher = null
        saveJob = viewModelScope.launch {
            isSaving = true
            val digits = pin.copy()
            try {
                createPin(digits, biometric?.takeIf { isBiometricAvailable })
                    .onSuccess {
                        pin.clear()
                        confirmation.clear()
                        navigate(CreatePinNavigation.PinCreated)
                    }
                    .onFailure { appError -> sendEffect(UiEffect.ShowSnackbar(appError.toUiText())) }
            } finally {
                digits.fill(Char.MIN_VALUE)
                isSaving = false
            }
        }
    }

    private companion object {
        const val STEPS_WITH_BIOMETRIC = 3
        const val STEPS_WITHOUT_BIOMETRIC = 2
    }
}
