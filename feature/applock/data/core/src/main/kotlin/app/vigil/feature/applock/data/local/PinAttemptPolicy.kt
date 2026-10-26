package app.vigil.feature.applock.data.local

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Five tries for free, then growing waits; the tenth wrong PIN erases the vault and signs out. */
internal object PinAttemptPolicy {
    const val MAX_ATTEMPTS = 10
    private const val FREE_ATTEMPTS = 4

    private val Delays = listOf(30.seconds, 1.minutes, 5.minutes, 15.minutes)

    /** The wait after the [failures]th wrong PIN in a row. */
    fun delayAfter(failures: Int): Duration =
        if (failures <= FREE_ATTEMPTS) Duration.ZERO else Delays[minOf(failures - FREE_ATTEMPTS, Delays.size) - 1]
}
