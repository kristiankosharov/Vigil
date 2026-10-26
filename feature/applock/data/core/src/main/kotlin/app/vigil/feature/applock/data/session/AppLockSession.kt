package app.vigil.feature.applock.data.session

import app.vigil.common.core.ApplicationScope
import app.vigil.common.security.VaultKeyControl
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Whether the app is locked right now. Lives only in memory: a new process starts locked. After
 * [GRACE_PERIOD] in the background it locks, so the vault key doesn't sit in memory while the app
 * waits there. Unlocking opens the vault with its key; locking closes it, wiping the key from memory.
 */
@Singleton
internal class AppLockSession @Inject constructor(
    private val timeSource: TimeSource,
    private val vault: VaultKeyControl,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val locked = MutableStateFlow(true)
    private var backgroundedAt: TimeMark? = null
    private var backgroundLock: Job? = null

    val isLocked: StateFlow<Boolean> = locked.asStateFlow()

    fun unlock(vaultKey: ByteArray) {
        vault.open(vaultKey)
        locked.value = false
    }

    fun lock() {
        vault.close()
        locked.value = true
    }

    fun onBackgrounded() {
        backgroundedAt = timeSource.markNow()
        backgroundLock?.cancel()
        backgroundLock = scope.launch {
            delay(GRACE_PERIOD)
            lock()
        }
    }

    fun onForegrounded() {
        backgroundLock?.cancel()
        backgroundLock = null
        val mark = backgroundedAt ?: return
        backgroundedAt = null
        // The timer can't fire while Android has the process frozen; check the elapsed time too.
        if (mark.elapsedNow() >= GRACE_PERIOD) lock()
    }

    companion object {
        val GRACE_PERIOD: Duration = 5.seconds
    }
}
