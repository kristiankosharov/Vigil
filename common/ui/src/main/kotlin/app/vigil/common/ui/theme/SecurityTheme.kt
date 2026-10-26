package app.vigil.common.ui.theme

import android.view.View
import android.view.Window
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.vigil.common.ui.R

/**
 * The security screens (PIN, fingerprint, unlock, scanner) always use the dark scheme, so they read
 * as a separate, protected space (docs/design/README.md). While any of them is shown, the system bar
 * icons are light so they stay visible; when the last one goes, they follow the app's theme again
 * ([AppSystemBars]), or the system theme if nothing set one. Several can be composed at once (the
 * lock screen over onboarding), so the window counts them.
 */
@Composable
fun SecurityTheme(content: @Composable () -> Unit) {
    val window = LocalActivity.current?.window
    val view = LocalView.current
    val systemDark = isSystemInDarkTheme()
    if (window != null && !view.isInEditMode) {
        DisposableEffect(window, view, systemDark) {
            window.changeSecurityScreens(delta = 1)
            window.updateSystemBars(view, systemDark)
            onDispose {
                window.changeSecurityScreens(delta = -1)
                window.updateSystemBars(view, systemDark)
            }
        }
    }
    AppTheme(darkTheme = true, content = content)
}

/**
 * Tells the window whether the app is in its dark theme, so the system bar icons stay readable when
 * the user picks light or dark against the system setting. Call it once, next to the app's
 * top-level [AppTheme]; security screens still win while they are shown.
 */
@Composable
fun AppSystemBars(darkTheme: Boolean) {
    val window = LocalActivity.current?.window
    val view = LocalView.current
    val systemDark = isSystemInDarkTheme()
    if (window != null && !view.isInEditMode) {
        DisposableEffect(window, view, darkTheme, systemDark) {
            window.decorView.setTag(R.id.common_app_dark_theme, darkTheme)
            window.updateSystemBars(view, systemDark)
            onDispose {
                window.decorView.setTag(R.id.common_app_dark_theme, null)
                window.updateSystemBars(view, systemDark)
            }
        }
    }
}

private fun Window.changeSecurityScreens(delta: Int) {
    val count = ((decorView.getTag(R.id.common_security_screens) as? Int) ?: 0) + delta
    decorView.setTag(R.id.common_security_screens, count)
}

private fun Window.updateSystemBars(view: View, systemDark: Boolean) {
    val securityScreens = (decorView.getTag(R.id.common_security_screens) as? Int) ?: 0
    val appDark = (decorView.getTag(R.id.common_app_dark_theme) as? Boolean) ?: systemDark
    val lightBars = securityScreens <= 0 && !appDark
    WindowCompat.getInsetsController(this, view).apply {
        isAppearanceLightStatusBars = lightBars
        isAppearanceLightNavigationBars = lightBars
    }
}
