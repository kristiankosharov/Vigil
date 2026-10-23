package app.vigil.feature.applock.data.session

/** Whether a strong biometric (class 3) is enrolled and usable; an interface so repositories test without Android. */
internal fun interface BiometricAvailability {
    fun isAvailable(): Boolean
}
