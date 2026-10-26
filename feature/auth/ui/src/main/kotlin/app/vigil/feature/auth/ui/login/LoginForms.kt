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
import app.vigil.feature.auth.domain.model.SignInIdentifierKind
import app.vigil.feature.auth.ui.R
import kotlinx.collections.immutable.ImmutableList

// The login screen's forms; LoginScreen.kt puts them together.

/**
 * One "email or phone number" field. An email shows the password field and signs in (or creates
 * the account); a phone number sends an SMS code. What was typed decides; there is no method switch.
 */
@Composable
internal fun SignInForm(state: LoginUiState, onAction: (LoginAction) -> Unit, modifier: Modifier = Modifier) {
    val submit = { onAction(LoginAction.Submit) }
    val isEmail = state.identifierKind == SignInIdentifierKind.Email
    val isPhone = state.identifierKind == SignInIdentifierKind.Phone
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.identifier,
            onValueChange = { onAction(LoginAction.IdentifierChanged(it)) },
            label = { Text(stringResource(R.string.auth_identifier_label)) },
            supportingText = {
                Text(stringResource(if (isPhone) R.string.auth_phone_hint else R.string.auth_identifier_hint))
            },
            singleLine = true,
            enabled = !state.isBusy,
            // The email keyboard has both "@" and digits; Next goes on to the password only for an email.
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = if (isEmail) ImeAction.Next else ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Username },
        )
        if (isEmail) PasswordField(state = state, onAction = onAction, onDone = submit)
        FormError(state.error)
        ProgressButton(
            text = stringResource(
                when {
                    isPhone -> R.string.auth_send_code_button
                    !isEmail -> R.string.auth_continue_button
                    state.emailMode == EmailMode.SignIn -> R.string.auth_sign_in_button
                    else -> R.string.auth_sign_up_button
                },
            ),
            isBusy = state.isBusy,
            enabled = state.identifier.isNotBlank(),
            onClick = submit,
            modifier = Modifier.fillMaxWidth(),
        )
        AccountLinks(state = state, showForgotPassword = isEmail, onAction = onAction)
    }
}

@Composable
private fun PasswordField(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = state.password,
        onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
        label = { Text(stringResource(R.string.auth_password_label)) },
        singleLine = true,
        enabled = !state.isBusy,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentType = if (state.emailMode == EmailMode.SignUp) ContentType.NewPassword else ContentType.Password
            },
    )
}

/** "No account? Create one" (or back to sign in), and "Forgot password?" for an email. */
@Composable
private fun AccountLinks(
    state: LoginUiState,
    showForgotPassword: Boolean,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
        if (showForgotPassword && state.emailMode == EmailMode.SignIn) {
            TextButton(onClick = { onAction(LoginAction.ForgotPassword) }, enabled = !state.isBusy) {
                Text(stringResource(R.string.auth_forgot_password))
            }
        }
    }
}

/** After a phone number: the SMS code. */
@Composable
internal fun CodeForm(state: LoginUiState, onAction: (LoginAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
private fun ProgressButton(
    text: String,
    isBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // While busy, the button itself says so: the spinner inside it is decorative.
    val inProgress = stringResource(R.string.auth_in_progress)
    Button(
        onClick = onClick,
        enabled = enabled && !isBusy,
        modifier = if (isBusy) modifier.semantics { contentDescription = inProgress } else modifier,
    ) {
        if (isBusy) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(text)
        }
    }
}
