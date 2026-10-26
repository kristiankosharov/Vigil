package app.vigil.feature.auth.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import app.vigil.feature.auth.ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun LoginRoute(onLoggedIn: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: LoginViewModel = hiltViewModel()
    LoginScreen(
        state = viewModel.uiState(),
        onAction = viewModel::onAction,
        onLoggedIn = onLoggedIn,
        modifier = modifier,
    )
}

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { onAction(LoginAction.EffectConsumed(it)) },
    )
    NavigationHandler(
        navigation = state.navigation,
        onNavigate = { navigation ->
            when (navigation) {
                LoginNavigation.LoggedIn -> onLoggedIn()
            }
        },
        onNavigationConsumed = { onAction(LoginAction.NavigationConsumed(it)) },
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header()
            MethodSelector(
                method = state.method,
                enabled = !state.isBusy,
                onSelected = { onAction(LoginAction.MethodSelected(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
            when (state.method) {
                LoginMethod.Email -> EmailForm(state = state, onAction = onAction)
                LoginMethod.Phone -> PhoneForm(state = state, onAction = onAction)
            }
            if (state.oidcProviders.isNotEmpty()) {
                OidcButtons(
                    providers = state.oidcProviders,
                    enabled = !state.isBusy,
                    onSelected = { onAction(LoginAction.OidcSelected(it)) },
                )
            }
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.auth_login_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.auth_login_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MethodSelector(
    method: LoginMethod,
    enabled: Boolean,
    onSelected: (LoginMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val methods = LoginMethod.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        methods.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == method,
                onClick = { onSelected(option) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = methods.size),
            ) {
                Text(
                    stringResource(
                        when (option) {
                            LoginMethod.Email -> R.string.auth_method_email
                            LoginMethod.Phone -> R.string.auth_method_phone
                        },
                    ),
                )
            }
        }
    }
}

private fun previewState(
    method: LoginMethod = LoginMethod.Email,
    phoneStep: PhoneStep = PhoneStep.EnterNumber,
    oidcProviders: ImmutableList<OidcProviderItem> = persistentListOf(),
    error: UiText? = null,
    isBusy: Boolean = false,
) = LoginUiState(
    method = method,
    emailMode = EmailMode.SignIn,
    email = "ana@example.com",
    password = "secret",
    phoneStep = phoneStep,
    phoneNumber = "+359888123456",
    code = "123",
    oidcProviders = oidcProviders,
    error = error,
    isBusy = isBusy,
    effect = null,
    navigation = null,
)

@PreviewLightDark
@PreviewFontScale
@Composable
private fun LoginScreenEmailPreview() {
    AppTheme(dynamicColor = false) {
        LoginScreen(
            state = previewState(
                oidcProviders = persistentListOf(OidcProviderItem("oidc.company", "Company SSO")),
                error = UiText.Resource(R.string.auth_error_invalid_credentials),
            ),
            onAction = {},
            onLoggedIn = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenPhoneCodePreview() {
    AppTheme(dynamicColor = false) {
        LoginScreen(
            state = previewState(method = LoginMethod.Phone, phoneStep = PhoneStep.EnterCode),
            onAction = {},
            onLoggedIn = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenBusyPreview() {
    AppTheme(dynamicColor = false) {
        LoginScreen(state = previewState(isBusy = true), onAction = {}, onLoggedIn = {})
    }
}
