package app.vigil.common.ui.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Performs navigation that a ViewModel requested after an async result. Consumes it **before**
 * navigating, so it never runs twice (e.g. after the screen comes back).
 */
@Composable
fun <N : UiNavigation> NavigationHandler(navigation: N?, onNavigate: (N) -> Unit, onNavigationConsumed: (N) -> Unit) {
    val currentOnNavigate by rememberUpdatedState(onNavigate)
    val currentOnNavigationConsumed by rememberUpdatedState(onNavigationConsumed)

    LaunchedEffect(navigation) {
        navigation ?: return@LaunchedEffect
        currentOnNavigationConsumed(navigation)
        currentOnNavigate(navigation)
    }
}
