package app.vigil.feature.auth.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.vigil.common.ui.text.UiText
import app.vigil.common.ui.text.asString
import app.vigil.feature.auth.ui.R
import kotlinx.collections.immutable.ImmutableList

// The login screen's forms; LoginScreen.kt puts them together.

@Composable
internal fun EmailForm(state: LoginUiState, onAction: (LoginAction) -> Unit, modifier: Modifier = Modifier) {
    val submit = { onAction(LoginAction.SubmitEmail) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.email,
            onValueChange = { onAction(LoginAction.EmailChanged(it)) },
            label = { Text(stringResource(R.string.auth_email_label)) },
            singleLine = true,
            enabled = !state.isBusy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.EmailAddress },
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            label = { Text(stringResource(R.string.auth_password_label)) },
            singleLine = true,
            enabled = !state.isBusy,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentType =
                        if (state.emailMode == EmailMode.SignUp) ContentType.NewPassword else ContentType.Password
                },
        )
        FormError(state.error)
        ProgressButton(
            text = stringResource(
                if (state.emailMode == EmailMode.SignIn) R.string.auth_sign_in_button else R.string.auth_sign_up_button,
            ),
            isBusy = state.isBusy,
            onClick = submit,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { onAction(LoginAction.EmailModeToggled) }, enabled = !state.isBusy) {
                Text(
                    stringResource(
                        if (state.emailMode == EmailMode.SignIn) {
                            R.string.auth_switch_to_sign_up
                        } else {
                            R.string.auth_switch_to_sign_in
                        },
                    ),
                )
            }
            if (state.emailMode == EmailMode.SignIn) {
                TextButton(onClick = { onAction(LoginAction.ForgotPassword) }, enabled = !state.isBusy) {
                    Text(stringResource(R.string.auth_forgot_password))
                }
            }
        }
    }
}

@Composable
internal fun PhoneForm(state: LoginUiState, onAction: (LoginAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state.phoneStep) {
            PhoneStep.EnterNumber -> {
                OutlinedTextField(
                    value = state.phoneNumber,
                    onValueChange = { onAction(LoginAction.PhoneNumberChanged(it)) },
                    label = { Text(stringResource(R.string.auth_phone_label)) },
                    supportingText = { Text(stringResource(R.string.auth_phone_hint)) },
                    singleLine = true,
                    enabled = !state.isBusy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAction(LoginAction.SendCode) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.PhoneNumber },
                )
                FormError(state.error)
                ProgressButton(
                    text = stringResource(R.string.auth_send_code_button),
                    isBusy = state.isBusy,
                    onClick = { onAction(LoginAction.SendCode) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PhoneStep.EnterCode -> {
                Text(
                    text = stringResource(R.string.auth_code_sent, state.phoneNumber),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = state.code,
                    onValueChange = { onAction(LoginAction.CodeChanged(it)) },
                    label = { Text(stringResource(R.string.auth_code_label)) },
                    singleLine = true,
                    enabled = !state.isBusy,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onAction(LoginAction.VerifyCode) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.SmsOtpCode },
                )
                FormError(state.error)
                ProgressButton(
                    text = stringResource(R.string.auth_verify_button),
                    isBusy = state.isBusy,
                    onClick = { onAction(LoginAction.VerifyCode) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { onAction(LoginAction.ChangePhoneNumber) }, enabled = !state.isBusy) {
                    Text(stringResource(R.string.auth_change_number))
                }
            }
        }
    }
}

// TODO(oidc): providers come from OidcProviders in :feature:auth:data:core; empty until one is set up.
@Composable
internal fun OidcButtons(
    providers: ImmutableList<OidcProviderItem>,
    enabled: Boolean,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(stringResource(R.string.auth_or), style = MaterialTheme.typography.labelMedium)
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        providers.forEach { provider ->
            OutlinedButton(
                onClick = { onSelected(provider.id) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.auth_oidc_button, provider.displayName))
            }
        }
    }
}

@Composable
private fun FormError(error: UiText?, modifier: Modifier = Modifier) {
    if (error == null) return
    Text(
        text = error.asString(),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ProgressButton(text: String, isBusy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // While busy, the button itself says so: the spinner inside it is decorative.
    val inProgress = stringResource(R.string.auth_in_progress)
    Button(
        onClick = onClick,
        enabled = !isBusy,
        modifier = if (isBusy) modifier.semantics { contentDescription = inProgress } else modifier,
    ) {
        if (isBusy) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(text)
        }
    }
}
