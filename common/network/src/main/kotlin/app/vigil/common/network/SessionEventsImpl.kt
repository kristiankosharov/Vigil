package app.vigil.common.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
internal class SessionEventsImpl @Inject constructor() : SessionEvents {
    private val events = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val expired: Flow<Unit> = events.asSharedFlow()

    override fun notifyExpired() {
        events.tryEmit(Unit)
    }
}
