package app.vigil

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import app.vigil.feature.settings.ui.theme.AppThemeHost
import app.vigil.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

/** A FragmentActivity because the app lock's BiometricPrompt needs one. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // No screenshots, screen recording or recents thumbnail: the app shows documents and the PIN
        // pad, and the thumbnail would show them even while the app is locked.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            AppThemeHost {
                // light, dark or system, as chosen on the Appearance screen
                AppNavigation()
            }
        }
    }
}
