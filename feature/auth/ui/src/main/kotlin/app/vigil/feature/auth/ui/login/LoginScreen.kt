package app.vigil.feature.auth.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.vigil.common.ui.mvi.NavigationHandler
import app.vigil.common.ui.mvi.UiEffectHandler
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.auth.ui.R

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

// TODO(auth): the real sign-in form; this placeholder has only the button.
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
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
            LoginButton(
                isLoggingIn = state.isLoggingIn,
                onClick = { onAction(LoginAction.Submit) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LoginButton(isLoggingIn: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // While logging in, the button itself says so: the spinner inside it is decorative.
    val inProgress = stringResource(R.string.auth_login_in_progress)
    Button(
        onClick = onClick,
        enabled = !isLoggingIn,
        modifier = if (isLoggingIn) modifier.semantics { contentDescription = inProgress } else modifier,
    ) {
        if (isLoggingIn) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(stringResource(R.string.auth_login_button))
        }
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun LoginScreenPreview() {
    AppTheme(dynamicColor = false) {
        LoginScreen(
            state = LoginUiState(isLoggingIn = false, effect = null, navigation = null),
            onAction = {},
            onLoggedIn = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenLoggingInPreview() {
    AppTheme(dynamicColor = false) {
        LoginScreen(
            state = LoginUiState(isLoggingIn = true, effect = null, navigation = null),
            onAction = {},
            onLoggedIn = {},
        )
    }
}
