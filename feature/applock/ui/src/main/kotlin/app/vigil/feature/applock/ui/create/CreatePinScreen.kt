package app.vigil.feature.applock.ui.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.vigil.common.ui.mvi.NavigationHandler
import app.vigil.common.ui.mvi.UiEffectHandler
import app.vigil.common.ui.text.UiText
import app.vigil.common.ui.text.asString
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.applock.domain.model.PinRules
import app.vigil.feature.applock.ui.R
import app.vigil.feature.applock.ui.components.PinDots
import app.vigil.feature.applock.ui.components.PinPad

@Composable
fun CreatePinRoute(onPinCreated: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: CreatePinViewModel = hiltViewModel()
    CreatePinScreen(
        state = viewModel.uiState(),
        onAction = viewModel::onAction,
        onPinCreated = onPinCreated,
        modifier = modifier,
    )
}

@Composable
internal fun CreatePinScreen(
    state: CreatePinUiState,
    onAction: (CreatePinAction) -> Unit,
    onPinCreated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { onAction(CreatePinAction.EffectConsumed(it)) },
    )
    NavigationHandler(
        navigation = state.navigation,
        onNavigate = { navigation ->
            when (navigation) {
                CreatePinNavigation.PinCreated -> onPinCreated()
            }
        },
        onNavigationConsumed = { onAction(CreatePinAction.NavigationConsumed(it)) },
    )
    // Back on the repeat step returns to the first step instead of leaving the screen.
    BackHandler(enabled = state.step == CreatePinStep.Repeat) { onAction(CreatePinAction.Back) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        CreatePinStepContent(
            state = state,
            onAction = onAction,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()) // 200% font scale on small screens
                .padding(24.dp),
        )
    }
}

@Composable
private fun CreatePinStepContent(
    state: CreatePinUiState,
    onAction: (CreatePinAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRepeat = state.step == CreatePinStep.Repeat
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(if (isRepeat) R.string.applock_repeat_title else R.string.applock_create_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = state.error?.asString()
                ?: stringResource(if (isRepeat) R.string.applock_repeat_message else R.string.applock_create_message),
            color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        PinDots(length = state.input.length, isError = state.error != null)
        PinPad(
            onDigit = { onAction(CreatePinAction.InputChanged(state.input + it)) },
            onDelete = { onAction(CreatePinAction.InputChanged(state.input.dropLast(1))) },
            isDeleteEnabled = state.input.isNotEmpty(),
            enabled = !state.isSaving,
        )
        if (isRepeat && state.isBiometricAvailable) {
            BiometricSwitch(
                checked = state.isBiometricEnabled,
                onCheckedChange = { onAction(CreatePinAction.BiometricToggled(it)) },
            )
        }
        Button(
            onClick = { onAction(if (isRepeat) CreatePinAction.Submit else CreatePinAction.Next) },
            enabled = !state.isSaving && state.input.length >= PinRules.MIN_LENGTH,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (isRepeat) R.string.applock_save else R.string.applock_next))
        }
    }
}

/** The whole row toggles, and reads as one switch to accessibility services. */
@Composable
private fun BiometricSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.applock_enable_biometric), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun previewState(step: CreatePinStep, error: UiText? = null) = CreatePinUiState(
    step = step,
    input = "12",
    isBiometricAvailable = true,
    isBiometricEnabled = true,
    error = error,
    isSaving = false,
    effect = null,
    navigation = null,
)

@PreviewLightDark
@PreviewFontScale
@Composable
private fun CreatePinEnterPreview() {
    AppTheme(dynamicColor = false) {
        CreatePinScreen(state = previewState(CreatePinStep.Enter), onAction = {}, onPinCreated = {})
    }
}

@PreviewLightDark
@Composable
private fun CreatePinRepeatPreview() {
    AppTheme(dynamicColor = false) {
        CreatePinScreen(state = previewState(CreatePinStep.Repeat), onAction = {}, onPinCreated = {})
    }
}

@PreviewLightDark
@Composable
private fun CreatePinMismatchPreview() {
    AppTheme(dynamicColor = false) {
        CreatePinScreen(
            state = previewState(CreatePinStep.Enter, UiText.Resource(R.string.applock_error_pin_mismatch)),
            onAction = {},
            onPinCreated = {},
        )
    }
}
