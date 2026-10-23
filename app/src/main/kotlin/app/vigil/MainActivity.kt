package app.vigil

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import app.vigil.common.ui.theme.AppTheme
import app.vigil.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

/** A FragmentActivity because the app lock's BiometricPrompt needs one. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The recents thumbnail would show the app's content even while it is locked.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
        setContent {
            AppTheme {
                AppNavigation()
            }
        }
    }
}
