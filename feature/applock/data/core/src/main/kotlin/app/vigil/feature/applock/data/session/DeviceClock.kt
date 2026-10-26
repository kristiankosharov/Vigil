package app.vigil.feature.applock.data.session

import kotlin.time.Duration

/**
 * Time since boot and the boot number. Lockouts use these instead of the wall clock, which the user
 * can change; an interface so the repository tests without Android.
 */
internal interface DeviceClock {
    fun sinceBoot(): Duration

    fun bootCount(): Int
}
