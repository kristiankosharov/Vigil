package app.vigil.common.ui.mvi

import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import app.vigil.common.ui.text.asString

/**
 * Shows the screen's current [effect] and reports it consumed once it was fully shown. The only
 * place in the app that shows toasts and snackbars. A snackbar interrupted by rotation or
 * navigation is shown again later (intended).
 */
@Composable
fun <A : UiAction> UiEffectHandler(
    effect: UiEffect<A>?,
    snackbarHostState: SnackbarHostState,
    onAction: (A) -> Unit,
    onEffectConsumed: (UiEffect<A>) -> Unit,
) {
    val context = LocalContext.current
    val currentOnAction by rememberUpdatedState(onAction)
    val currentOnEffectConsumed by rememberUpdatedState(onEffectConsumed)

    LaunchedEffect(effect) {
        when (effect) {
            null -> return@LaunchedEffect

            is UiEffect.ShowToast -> Toast.makeText(
                context.applicationContext, // never keep the Activity alive
                effect.message.asString(context),
                if (effect.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
            ).show()

            is UiEffect.ShowSnackbar -> {
                val result = snackbarHostState.showSnackbar(
                    message = effect.message.asString(context),
                    actionLabel = effect.actionLabel?.asString(context),
                    duration = effect.duration,
                )
                if (result == SnackbarResult.ActionPerformed) effect.action?.let(currentOnAction)
            }
        }
        currentOnEffectConsumed(effect)
    }
}
