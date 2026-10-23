package app.vigil.feature.applock.ui.biometric

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.vigil.feature.applock.ui.R

/**
 * Shows the system biometric prompt for [request] and reports success. Consumes the request before
 * showing it, like NavigationHandler. Cancelling or failing leaves the PIN entry on screen.
 */
@Composable
internal fun BiometricPromptHandler(
    request: BiometricRequest?,
    onSucceeded: () -> Unit,
    onRequestConsumed: (BiometricRequest) -> Unit,
) {
    val activity = LocalActivity.current as? FragmentActivity // BiometricPrompt needs a FragmentActivity
    val title = stringResource(R.string.applock_biometric_title)
    val usePin = stringResource(R.string.applock_biometric_use_pin)
    val currentOnSucceeded by rememberUpdatedState(onSucceeded)
    val currentOnRequestConsumed by rememberUpdatedState(onRequestConsumed)

    LaunchedEffect(request) {
        request ?: return@LaunchedEffect
        currentOnRequestConsumed(request)
        activity ?: return@LaunchedEffect
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                currentOnSucceeded()
            }
        }
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText(usePin)
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .build(),
        )
    }
}
