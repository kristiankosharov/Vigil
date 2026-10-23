package app.vigil.common.ui.mvi

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * FIFO of one-shot effects, kept in snapshot state so `uiState()` recomposes when the head changes.
 * Safe from any thread: updates run under a coroutine [Mutex]. Used only by [BaseViewModel].
 */
@Stable
class UiEffectQueue<A : UiAction> {
    private val mutex = Mutex()
    private var effects by mutableStateOf(persistentListOf<UiEffect<A>>())

    /** The effect to show now: the oldest one not yet consumed. */
    val current: UiEffect<A>? get() = effects.firstOrNull()

    suspend fun send(effect: UiEffect<A>) = mutex.withLock {
        effects = effects.adding(effect)
    }

    /** Removes [effect] only if it is still the head (identity), so a stale consume is a no-op. */
    suspend fun consume(effect: UiEffect<A>) = mutex.withLock {
        if (effects.firstOrNull() === effect) effects = effects.removingAt(0)
    }
}
