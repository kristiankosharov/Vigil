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
 * icons are light so they stay visible; when the last one goes, they follow the system theme again.
 * Several can be composed at once (the lock screen over onboarding), so the window counts them.
 */
@Composable
fun SecurityTheme(content: @Composable () -> Unit) {
    val window = LocalActivity.current?.window
    val view = LocalView.current
    val systemDark = isSystemInDarkTheme()
    if (window != null && !view.isInEditMode) {
        DisposableEffect(window, view, systemDark) {
            window.changeSecurityScreens(view, delta = 1, systemDark = systemDark)
            onDispose { window.changeSecurityScreens(view, delta = -1, systemDark = systemDark) }
        }
    }
    AppTheme(darkTheme = true, content = content)
}

private fun Window.changeSecurityScreens(view: View, delta: Int, systemDark: Boolean) {
    val count = ((decorView.getTag(R.id.common_security_screens) as? Int) ?: 0) + delta
    decorView.setTag(R.id.common_security_screens, count)
    val lightBars = count <= 0 && !systemDark
    WindowCompat.getInsetsController(this, view).apply {
        isAppearanceLightStatusBars = lightBars
        isAppearanceLightNavigationBars = lightBars
    }
}
