package app.vigil.feature.auth.ui.session

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.vigil.common.ui.mvi.NavigationHandler

/**
 * Wraps the app's navigation and calls [onSessionExpired] when the session ends on the server side
 * (the app then clears the back stack and opens the login screen).
 */
@Composable
fun AuthSessionHost(onSessionExpired: () -> Unit, content: @Composable () -> Unit) {
    val viewModel: AuthSessionViewModel = hiltViewModel()
    val state = viewModel.uiState()
    NavigationHandler(
        navigation = state.navigation,
        onNavigate = { navigation ->
            when (navigation) {
                AuthSessionNavigation.SessionExpired -> onSessionExpired()
            }
        },
        onNavigationConsumed = { viewModel.onAction(AuthSessionAction.NavigationConsumed(it)) },
    )
    content()
}
