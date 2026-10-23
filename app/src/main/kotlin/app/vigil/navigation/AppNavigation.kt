package app.vigil.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.vigil.feature.applock.ui.create.CreatePinRoute
import app.vigil.feature.applock.ui.lock.AppLockHost
import app.vigil.feature.auth.ui.login.LoginRoute
import app.vigil.feature.auth.ui.session.AuthSessionHost
import app.vigil.feature.home.ui.HomeRoute

/**
 * The app's whole navigation: the back stack and one entry per destination. AppLockHost is the
 * outermost layer, so the lock screen covers everything, the session-expiry handling included.
 */
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    // TODO(auth): start at HomeKey when a session already exists.
    val backStack = rememberNavBackStack(LoginKey)
    AppLockHost {
        AuthSessionHost(onSessionExpired = { backStack.replaceAllWith(LoginKey) }) {
            NavDisplay(
                backStack = backStack,
                modifier = modifier,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(), // each destination gets its own ViewModels
                ),
                entryProvider = entryProvider {
                    entry<LoginKey> { LoginRoute(onLoggedIn = { backStack.replaceAllWith(CreatePinKey) }) }
                    entry<CreatePinKey> { CreatePinRoute(onPinCreated = { backStack.replaceAllWith(HomeKey) }) }
                    entry<HomeKey> { HomeRoute() }
                },
            )
        }
    }
}

/** Opens [key] as the only destination (e.g. Home after login, Login after session expiry). */
private fun NavBackStack<NavKey>.replaceAllWith(key: NavKey) {
    add(key)
    removeAll { it != key }
}
