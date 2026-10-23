package app.vigil.common.ui.mvi

import androidx.compose.material3.SnackbarDuration
import app.vigil.common.ui.text.UiText

/**
 * A one-shot message, shown by [UiEffectHandler] and then consumed.
 * Plain classes (not data classes) on purpose: identity equality, so the same message sent twice in
 * a row is shown twice.
 */
sealed interface UiEffect<out A : UiAction> {
    class ShowToast(val message: UiText, val isLong: Boolean = false) : UiEffect<Nothing>

    class ShowSnackbar<out A : UiAction>(
        val message: UiText,
        val actionLabel: UiText? = null,
        val action: A? = null, // dispatched back to the ViewModel when tapped
        val duration: SnackbarDuration = SnackbarDuration.Short,
    ) : UiEffect<A>
}
