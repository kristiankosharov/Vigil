package app.vigil.feature.applock.data.session

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the app is locked right now. Lives only in memory: a new process starts locked. After the
 * app was in the background for [GRACE_PERIOD] or longer it locks again when it comes back.
 */
@Singleton
internal class AppLockSession @Inject constructor(private val timeSource: TimeSource) {
    private val locked = MutableStateFlow(true)
    private var backgroundedAt: TimeMark? = null

    val isLocked: StateFlow<Boolean> = locked.asStateFlow()

    fun unlock() {
        locked.value = false
    }

    fun onBackgrounded() {
        backgroundedAt = timeSource.markNow()
    }

    fun onForegrounded() {
        val mark = backgroundedAt ?: return
        backgroundedAt = null
        if (mark.elapsedNow() >= GRACE_PERIOD) locked.value = true
    }

    companion object {
        val GRACE_PERIOD: Duration = 30.seconds
    }
}
