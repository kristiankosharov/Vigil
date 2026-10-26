package app.vigil.feature.applock.data.repository

import androidx.datastore.core.DataStore
import app.vigil.common.core.UserDataCleaner
import app.vigil.common.network.SessionEvents
import app.vigil.common.security.BiometricKeys
import app.vigil.feature.applock.data.PinCheckData
import app.vigil.feature.applock.data.local.AppLockSettings
import app.vigil.feature.applock.data.local.Lockout
import app.vigil.feature.applock.data.local.PinAttemptPolicy
import app.vigil.feature.applock.data.local.PinWrappedKey
import app.vigil.feature.applock.data.local.VaultKeyWrapper
import app.vigil.feature.applock.data.session.AppLockSession
import app.vigil.feature.applock.data.session.DeviceClock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * PIN checks with the attempt limit (PinAttemptPolicy): waits after too many wrong PINs, and the
 * last wrong PIN erases every user's data and signs out.
 */
@Singleton
internal class PinAttempts @Inject constructor(
    private val store: DataStore<AppLockSettings>,
    private val wrapper: VaultKeyWrapper,
    private val session: AppLockSession,
    private val biometricKeys: BiometricKeys,
    private val clock: DeviceClock,
    private val cleaners: Provider<Set<@JvmSuppressWildcards UserDataCleaner>>,
    private val sessionEvents: SessionEvents,
) {
    // One check at a time, so two quick submits can't both slip past the count.
    private val mutex = Mutex()

    suspend fun check(pin: CharArray): PinCheckData = mutex.withLock {
        val settings = store.data.first()
        val pinKey = settings.pinKey
        val wait = remainingLockout(settings)
        when {
            pinKey == null -> PinCheckData.Wrong(PinAttemptPolicy.MAX_ATTEMPTS, Duration.ZERO)
            wait > Duration.ZERO -> PinCheckData.Wrong(attemptsLeft(settings.failedAttempts), wait)
            else -> tryPin(pinKey, pin)
        }
    }

    /** How long PIN entry stays blocked. After a reboot the time since boot restarts: the full wait again. */
    fun remainingLockout(settings: AppLockSettings): Duration {
        val lockout = settings.lockout
        return when {
            lockout == null -> Duration.ZERO
            lockout.bootCount != clock.bootCount() -> PinAttemptPolicy.delayAfter(settings.failedAttempts)
            else -> (lockout.untilElapsedMillis.milliseconds - clock.sinceBoot()).coerceAtLeast(Duration.ZERO)
        }
    }

    private suspend fun tryPin(pinKey: PinWrappedKey, pin: CharArray): PinCheckData {
        // Counted before the check: killing the app mid-check can't give a free try.
        val failures = store.updateData { it.copy(failedAttempts = it.failedAttempts + 1) }.failedAttempts
        val vaultKey = wrapper.unwrapWithPin(pinKey, pin)
        return when {
            vaultKey != null -> {
                store.updateData { it.copy(failedAttempts = 0, lockout = null) }
                session.unlock(vaultKey)
                vaultKey.fill(0)
                PinCheckData.Correct
            }

            failures >= PinAttemptPolicy.MAX_ATTEMPTS -> {
                erase()
                PinCheckData.Erased
            }

            else -> {
                val delay = PinAttemptPolicy.delayAfter(failures)
                if (delay > Duration.ZERO) {
                    val until = (clock.sinceBoot() + delay).inWholeMilliseconds
                    store.updateData { it.copy(lockout = Lockout(clock.bootCount(), until)) }
                }
                PinCheckData.Wrong(attemptsLeft(failures), delay)
            }
        }
    }

    /** Every user's data goes (this store included) and the user is signed out. */
    private suspend fun erase() {
        session.lock()
        biometricKeys.delete()
        cleaners.get().forEach { it.clear() }
        sessionEvents.notifyExpired()
    }

    private fun attemptsLeft(failures: Int) = PinAttemptPolicy.MAX_ATTEMPTS - failures
}
