package app.vigil.feature.applock.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.vigil.common.ui.mvi.NavigationHandler
import app.vigil.common.ui.mvi.UiEffectHandler
import app.vigil.common.ui.text.UiText
import app.vigil.common.ui.text.asString
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.applock.ui.R

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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        CreatePinForm(
            state = state,
            onAction = onAction,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        )
    }
}

@Composable
private fun CreatePinForm(state: CreatePinUiState, onAction: (CreatePinAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.applock_create_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = stringResource(R.string.applock_create_message), style = MaterialTheme.typography.bodyLarge)
        PinField(
            value = state.pin,
            label = stringResource(R.string.applock_pin_label),
            onValueChange = { onAction(CreatePinAction.PinChanged(it)) },
            isError = state.error != null,
            imeAction = ImeAction.Next,
        )
        PinField(
            value = state.confirmPin,
            label = stringResource(R.string.applock_confirm_pin_label),
            onValueChange = { onAction(CreatePinAction.ConfirmPinChanged(it)) },
            isError = state.error != null,
            imeAction = ImeAction.Done,
        )
        state.error?.let { error ->
            Text(
                text = error.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.isBiometricAvailable) {
            BiometricSwitch(
                checked = state.isBiometricEnabled,
                onCheckedChange = { onAction(CreatePinAction.BiometricToggled(it)) },
            )
        }
        Button(
            onClick = { onAction(CreatePinAction.Submit) },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.applock_save))
        }
    }
}

@Composable
private fun PinField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
        modifier = modifier.fillMaxWidth(),
    )
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

private fun previewState(error: UiText?) = CreatePinUiState(
    pin = "12",
    confirmPin = "",
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
private fun CreatePinScreenPreview() {
    AppTheme(dynamicColor = false) {
        CreatePinScreen(state = previewState(error = null), onAction = {}, onPinCreated = {})
    }
}

@PreviewLightDark
@Composable
private fun CreatePinScreenErrorPreview() {
    AppTheme(dynamicColor = false) {
        CreatePinScreen(
            state = previewState(error = UiText.Resource(R.string.applock_error_pin_mismatch)),
            onAction = {},
            onPinCreated = {},
        )
    }
}
