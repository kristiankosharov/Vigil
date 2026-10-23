package app.vigil.common.ui.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Base for every screen's ViewModel. State lives in private snapshot fields; [uiState] builds the
 * immutable state from them; [onAction] is the single entry point. Effects and result navigation are
 * consumable state, exposed through [currentEffect] / [currentNavigation] inside [uiState].
 */
abstract class BaseViewModel<S : UiState, A : UiAction, N : UiNavigation> : ViewModel() {

    private val effects = UiEffectQueue<A>()
    private var navigation by mutableStateOf<N?>(null)

    /** The effect waiting to be shown; read it inside [uiState]. */
    protected val currentEffect: UiEffect<A>? get() = effects.current

    /** The navigation waiting to happen; read it inside [uiState]. */
    protected val currentNavigation: N? get() = navigation

    /** Builds the screen state from the snapshot fields. Pure: no side effects, no use case calls. */
    @Composable
    abstract fun uiState(): S

    abstract fun onAction(action: A)

    protected fun sendEffect(effect: UiEffect<A>) {
        viewModelScope.launch { effects.send(effect) }
    }

    protected fun consumeEffect(effect: UiEffect<A>) {
        viewModelScope.launch { effects.consume(effect) }
    }

    /** Main thread only, like every other snapshot field. */
    protected fun navigate(target: N) {
        navigation = target
    }

    protected fun consumeNavigation(target: N) {
        if (navigation === target) navigation = null
    }
}
