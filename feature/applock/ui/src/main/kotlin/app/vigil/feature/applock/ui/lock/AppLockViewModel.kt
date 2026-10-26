package app.vigil.feature.applock.ui.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import app.vigil.common.core.onFailure
import app.vigil.common.core.onSuccess
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.mvi.BaseViewModel
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.text.UiText
import app.vigil.common.ui.text.toUiText
import app.vigil.feature.applock.domain.model.AppLockState
import app.vigil.feature.applock.domain.model.PinRules
import app.vigil.feature.applock.domain.usecase.MarkAppBackgroundedUseCase
import app.vigil.feature.applock.domain.usecase.MarkAppForegroundedUseCase
import app.vigil.feature.applock.domain.usecase.ObserveAppLockStateUseCase
import app.vigil.feature.applock.domain.usecase.UnlockWithBiometricUseCase
import app.vigil.feature.applock.domain.usecase.UnlockWithPinUseCase
import app.vigil.feature.applock.ui.biometric.BiometricRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Activity-scoped (AppLockHost sits above navigation): survives rotation, and a new process gets a
 * new ViewModel that starts covered until the lock state is known.
 */
@HiltViewModel
internal class AppLockViewModel @Inject constructor(
    private val observeLockState: ObserveAppLockStateUseCase,
    private val unlockWithPin: UnlockWithPinUseCase,
    private val unlockWithBiometric: UnlockWithBiometricUseCase,
    private val markAppBackgrounded: MarkAppBackgroundedUseCase,
    private val markAppForegrounded: MarkAppForegroundedUseCase,
) : BaseViewModel<AppLockUiState, AppLockAction, Nothing>() {

    private var status by mutableStateOf(AppLockStatus.Loading)
    private var isBiometricEnabled by mutableStateOf(false)

    // Never savedStateHandle: the PIN must not be written to the saved instance state.
    private var pin by mutableStateOf("")
    private var isPinWrong by mutableStateOf(false)
    private var isChecking by mutableStateOf(false)
    private var biometricRequest by mutableStateOf<BiometricRequest?>(null)
    private var observeJob: Job? = null
    private var unlockJob: Job? = null

    init {
        observe()
    }

    @Composable
    override fun uiState() = AppLockUiState(
        status = status,
        pin = pin,
        isPinWrong = isPinWrong,
        isChecking = isChecking,
        isBiometricEnabled = isBiometricEnabled,
        biometricRequest = biometricRequest,
        effect = currentEffect,
    )

    override fun onAction(action: AppLockAction) {
        when (action) {
            is AppLockAction.PinChanged -> changePin(action.pin)

            AppLockAction.UseBiometric -> biometricRequest = BiometricRequest()

            AppLockAction.BiometricSucceeded -> unlockWithBiometric()

            is AppLockAction.BiometricRequestConsumed ->
                if (biometricRequest === action.request) biometricRequest = null

            AppLockAction.AppBackgrounded -> markAppBackgrounded()

            AppLockAction.AppForegrounded -> markAppForegrounded()

            AppLockAction.Retry -> observe()

            is AppLockAction.EffectConsumed -> consumeEffect(action.effect)
        }
    }

    private fun observe() {
        if (observeJob?.isActive == true) return
        observeJob = observeLockState()
            .onEach { result ->
                result
                    .onSuccess(::apply)
                    .onFailure { error ->
                        // Fail closed: if the lock state can't be read, the app stays covered.
                        if (status == AppLockStatus.Loading) status = AppLockStatus.Locked
                        sendEffect(
                            UiEffect.ShowSnackbar(
                                message = error.toUiText(),
                                actionLabel = UiText.Resource(CommonR.string.common_retry),
                                action = AppLockAction.Retry,
                            ),
                        )
                    }
            }
            .launchIn(viewModelScope)
    }

    private fun apply(state: AppLockState) {
        val becameLocked = state.isLocked && status != AppLockStatus.Locked
        isBiometricEnabled = state.isBiometricEnabled
        status = if (state.isLocked) AppLockStatus.Locked else AppLockStatus.Unlocked
        if (becameLocked) {
            pin = ""
            isPinWrong = false
            if (state.isBiometricEnabled) biometricRequest = BiometricRequest() // offer it right away
        }
        if (!state.isLocked) pin = ""
    }

    private fun changePin(value: String) {
        if (isChecking || value.length > PinRules.LENGTH || !value.all(Char::isDigit)) return
        pin = value
        isPinWrong = false
        if (value.length == PinRules.LENGTH) submitPin()
    }

    // TODO(applock): limit wrong attempts (growing delay, then log out).
    private fun submitPin() {
        if (unlockJob?.isActive == true || pin.length < PinRules.LENGTH) return
        unlockJob = viewModelScope.launch {
            isChecking = true
            try {
                unlockWithPin(pin)
                    .onSuccess { matched ->
                        if (!matched) {
                            isPinWrong = true
                            pin = ""
                        }
                    }
                    .onFailure { error -> sendEffect(UiEffect.ShowSnackbar(error.toUiText())) }
            } finally {
                isChecking = false
            }
        }
    }
}
