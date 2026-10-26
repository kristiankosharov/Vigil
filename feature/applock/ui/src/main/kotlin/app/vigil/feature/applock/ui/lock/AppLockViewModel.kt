package app.vigil.feature.applock.ui.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import app.vigil.common.core.AppError
import app.vigil.common.core.onFailure
import app.vigil.common.core.onSuccess
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.mvi.BaseViewModel
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.text.UiText
import app.vigil.common.ui.text.toUiText
import app.vigil.feature.applock.domain.model.AppLockState
import app.vigil.feature.applock.domain.model.BiometricCipher
import app.vigil.feature.applock.domain.model.PinCheck
import app.vigil.feature.applock.domain.usecase.MarkAppBackgroundedUseCase
import app.vigil.feature.applock.domain.usecase.MarkAppForegroundedUseCase
import app.vigil.feature.applock.domain.usecase.ObserveAppLockStateUseCase
import app.vigil.feature.applock.domain.usecase.PrepareBiometricUnlockUseCase
import app.vigil.feature.applock.domain.usecase.UnlockWithBiometricUseCase
import app.vigil.feature.applock.domain.usecase.UnlockWithPinUseCase
import app.vigil.feature.applock.ui.biometric.BiometricRequest
import app.vigil.feature.applock.ui.components.PinBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val prepareBiometricUnlock: PrepareBiometricUnlockUseCase,
    private val unlockWithBiometric: UnlockWithBiometricUseCase,
    private val markAppBackgrounded: MarkAppBackgroundedUseCase,
    private val markAppForegrounded: MarkAppForegroundedUseCase,
) : BaseViewModel<AppLockUiState, AppLockAction, Nothing>() {

    private var status by mutableStateOf(AppLockStatus.Loading)
    private var isBiometricEnabled by mutableStateOf(false)

    // Never savedStateHandle: the PIN must not be written to the saved instance state.
    private val pin = PinBuffer()
    private var isPinWrong by mutableStateOf(false)
    private var attemptsLeft by mutableStateOf<Int?>(null)
    private var retrySeconds by mutableIntStateOf(0)
    private var isChecking by mutableStateOf(false)
    private var biometricRequest by mutableStateOf<BiometricRequest?>(null)

    /** The cipher of the last prompt shown; it unlocks once BiometricPrompt reports success. */
    private var promptedCipher: BiometricCipher? = null
    private var observeJob: Job? = null
    private var unlockJob: Job? = null
    private var countdownJob: Job? = null

    init {
        observe()
    }

    @Composable
    override fun uiState() = AppLockUiState(
        status = status,
        pinLength = pin.length,
        isPinWrong = isPinWrong,
        attemptsLeft = attemptsLeft,
        retrySeconds = retrySeconds,
        isChecking = isChecking,
        isBiometricEnabled = isBiometricEnabled,
        biometricRequest = biometricRequest,
        effect = currentEffect,
    )

    override fun onAction(action: AppLockAction) {
        when (action) {
            is AppLockAction.Digit -> addDigit(action.digit)

            AppLockAction.Delete -> if (!isChecking) pin.delete()

            AppLockAction.UseBiometric -> requestBiometric()

            AppLockAction.BiometricSucceeded -> biometricUnlock()

            is AppLockAction.BiometricRequestConsumed -> if (biometricRequest === action.request) {
                promptedCipher = action.request.cipher
                biometricRequest = null
            }

            AppLockAction.AppBackgrounded -> {
                if (!isChecking) pin.clear() // nothing half-typed waits in memory
                markAppBackgrounded()
            }

            AppLockAction.AppForegrounded -> markAppForegrounded()

            AppLockAction.Retry -> observe()

            is AppLockAction.EffectConsumed -> consumeEffect(action.effect)
        }
    }

    override fun onCleared() {
        pin.clear()
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
        startCountdown(state.retryAfter)
        if (becameLocked) {
            pin.clear()
            isPinWrong = false
            if (state.isBiometricEnabled && retrySeconds == 0) requestBiometric() // offer it right away
        }
        if (!state.isLocked) {
            pin.clear()
            attemptsLeft = null
        }
    }

    private fun addDigit(digit: Int) {
        if (isChecking || retrySeconds > 0) return
        if (isPinWrong) isPinWrong = false
        pin.add(digit)
        if (pin.isFull) submitPin()
    }

    private fun submitPin() {
        if (unlockJob?.isActive == true) return
        unlockJob = viewModelScope.launch {
            isChecking = true
            val attempt = pin.copy()
            try {
                unlockWithPin(attempt)
                    .onSuccess { check ->
                        when (check) {
                            is PinCheck.Wrong -> {
                                isPinWrong = true
                                attemptsLeft = check.attemptsLeft
                                startCountdown(check.retryAfter)
                            }

                            // Erased: the lock state and the session-expiry handling take the user to sign-in.
                            PinCheck.Correct, PinCheck.Erased -> attemptsLeft = null
                        }
                    }
                    .onFailure { error -> sendEffect(UiEffect.ShowSnackbar(error.toUiText())) }
            } finally {
                attempt.fill(Char.MIN_VALUE)
                pin.clear()
                isChecking = false
            }
        }
    }

    private fun startCountdown(wait: Duration) {
        countdownJob?.cancel()
        retrySeconds = wait.inWholeSeconds.toInt() + if (wait.inWholeMilliseconds % MILLIS_PER_SECOND > 0) 1 else 0
        if (retrySeconds == 0) return
        countdownJob = viewModelScope.launch {
            while (retrySeconds > 0) {
                delay(1.seconds)
                retrySeconds--
            }
        }
    }

    private fun requestBiometric() {
        viewModelScope.launch {
            prepareBiometricUnlock()
                .onSuccess { cipher ->
                    if (cipher != null) biometricRequest = BiometricRequest(cipher) else isBiometricEnabled = false
                }
                .onFailure { error -> sendEffect(UiEffect.ShowSnackbar(error.toUiText())) }
        }
    }

    private fun biometricUnlock() {
        val cipher = promptedCipher ?: return
        promptedCipher = null
        viewModelScope.launch {
            unlockWithBiometric(cipher).onFailure { error: AppError ->
                sendEffect(UiEffect.ShowSnackbar(error.toUiText()))
            }
        }
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000
    }
}
