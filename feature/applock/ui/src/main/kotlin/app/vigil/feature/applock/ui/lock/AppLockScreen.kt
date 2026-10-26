package app.vigil.feature.applock.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.mvi.UiEffectHandler
import app.vigil.common.ui.theme.SecurityTheme
import app.vigil.feature.applock.domain.model.PinRules
import app.vigil.feature.applock.ui.R
import app.vigil.feature.applock.ui.biometric.BiometricPromptHandler
import app.vigil.feature.applock.ui.components.PinDots
import app.vigil.feature.applock.ui.components.PinDotsStatus
import app.vigil.feature.applock.ui.components.PinPad

/**
 * Unlock (docs/design/README.md, 1.4). Opaque, so nothing of the app shows through. While the state
 * is loading it shows nothing else.
 *
 * TODO(applock): "Forgot PIN?" and "Sign in with password" from the design need sign-out + PIN reset.
 */
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
        title = stringResource(R.string.applock_biometric_title),
        negativeText = stringResource(R.string.applock_biometric_use_pin),
        onSucceeded = { onAction(AppLockAction.BiometricSucceeded) },
        onRequestConsumed = { onAction(AppLockAction.BiometricRequestConsumed(it)) },
    )

    SecurityTheme {
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
                        .verticalScroll(rememberScrollState()) // 200% font scale on small screens
                        .padding(start = 32.dp, end = 32.dp, top = 72.dp, bottom = 40.dp),
                )
            }
        }
    }
}

@Composable
private fun PinEntry(state: AppLockUiState, onAction: (AppLockAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LockBadge()
        Text(
            text = stringResource(R.string.applock_locked_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 24.dp, bottom = 8.dp)
                .semantics { heading() },
        )
        Text(
            text = stringResource(R.string.applock_locked_message, PinRules.LENGTH),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PinDots(
            length = state.pin.length,
            status = if (state.isPinWrong) PinDotsStatus.Error else PinDotsStatus.Typing,
            modifier = Modifier.padding(top = 40.dp, bottom = 12.dp),
        )
        Text(
            text = when {
                state.isChecking -> stringResource(R.string.applock_unlocking)
                state.isPinWrong -> stringResource(R.string.applock_pin_wrong)
                else -> ""
            },
            color = with(MaterialTheme.colorScheme) { if (state.isPinWrong) error else onSurfaceVariant },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .heightIn(min = 20.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        PinPad(
            onDigit = { onAction(AppLockAction.PinChanged(state.pin + it)) },
            onDelete = { onAction(AppLockAction.PinChanged(state.pin.dropLast(1))) },
            isDeleteEnabled = state.pin.isNotEmpty(),
            enabled = !state.isChecking,
            onBiometric = if (state.isBiometricEnabled) ({ onAction(AppLockAction.UseBiometric) }) else null,
            modifier = Modifier.padding(top = 32.dp),
        )
    }
}

@Composable
private fun LockBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_shield_lock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
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
    AppLockScreen(state = previewState(isPinWrong = false), onAction = {})
}

@PreviewLightDark
@Composable
private fun AppLockScreenWrongPinPreview() {
    AppLockScreen(state = previewState(isPinWrong = true), onAction = {})
}
