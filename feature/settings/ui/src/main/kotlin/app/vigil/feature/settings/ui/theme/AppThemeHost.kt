package app.vigil.feature.settings.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.vigil.common.ui.theme.AppSystemBars
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.settings.domain.model.ThemeMode

/**
 * The app's theme, chosen on the Appearance screen: light, dark or the system setting. Wraps the
 * whole app; the security screens inside still switch to dark with `SecurityTheme`.
 */
@Composable
fun AppThemeHost(content: @Composable () -> Unit) {
    val viewModel: AppThemeViewModel = hiltViewModel()
    AppThemeLayer(state = viewModel.uiState(), content = content)
}

@Composable
internal fun AppThemeLayer(state: AppThemeUiState, content: @Composable () -> Unit) {
    val darkTheme = when (state.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    AppSystemBars(darkTheme = darkTheme)
    AppTheme(darkTheme = darkTheme, content = content)
}
