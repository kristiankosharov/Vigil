package app.vigil.feature.applock.domain.model

/** What makes a valid PIN: digits only, [MIN_LENGTH] to [MAX_LENGTH] of them. */
public object PinRules {
    public const val MIN_LENGTH: Int = 4
    public const val MAX_LENGTH: Int = 8

    public fun isValid(pin: String): Boolean = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all(Char::isDigit)
}
