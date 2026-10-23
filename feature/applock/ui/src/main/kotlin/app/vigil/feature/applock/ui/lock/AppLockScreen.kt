package app.vigil.feature.applock.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.vigil.common.ui.mvi.UiEffectHandler
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.applock.domain.model.PinRules
import app.vigil.feature.applock.ui.R
import app.vigil.feature.applock.ui.biometric.BiometricPromptHandler

/** Opaque, so nothing of the app shows through. While the state is loading it shows nothing else. */
@Composable
internal fun AppLockScreen(state: AppLockUiState, onAction: (AppLockAction) -> Unit, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }

    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { onAction(AppLockAction.EffectConsumed(it)) },
    )
    BiometricPromptHandler(
        request = state.biometricRequest,
        onSucceeded = { onAction(AppLockAction.BiometricSucceeded) },
        onRequestConsumed = { onAction(AppLockAction.BiometricRequestConsumed(it)) },
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.status == AppLockStatus.Locked) {
            PinEntry(
                state = state,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            )
        }
    }
}

@Composable
private fun PinEntry(state: AppLockUiState, onAction: (AppLockAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.applock_locked_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.applock_locked_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        OutlinedTextField(
            value = state.pin,
            onValueChange = { onAction(AppLockAction.PinChanged(it)) },
            label = { Text(stringResource(R.string.applock_pin_label)) },
            isError = state.isPinWrong,
            supportingText = if (state.isPinWrong) {
                { Text(stringResource(R.string.applock_pin_wrong)) }
            } else {
                null
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAction(AppLockAction.SubmitPin) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onAction(AppLockAction.SubmitPin) },
            enabled = !state.isChecking && state.pin.length >= PinRules.MIN_LENGTH,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.applock_unlock))
        }
        if (state.isBiometricEnabled) {
            TextButton(onClick = { onAction(AppLockAction.UseBiometric) }) {
                Text(stringResource(R.string.applock_use_biometric))
            }
        }
    }
}

private fun previewState(isPinWrong: Boolean) = AppLockUiState(
    status = AppLockStatus.Locked,
    pin = if (isPinWrong) "" else "12",
    isPinWrong = isPinWrong,
    isChecking = false,
    isBiometricEnabled = true,
    biometricRequest = null,
    effect = null,
)

@PreviewLightDark
@PreviewFontScale
@Composable
private fun AppLockScreenPreview() {
    AppTheme(dynamicColor = false) { AppLockScreen(state = previewState(isPinWrong = false), onAction = {}) }
}

@PreviewLightDark
@Composable
private fun AppLockScreenWrongPinPreview() {
    AppTheme(dynamicColor = false) { AppLockScreen(state = previewState(isPinWrong = true), onAction = {}) }
}
