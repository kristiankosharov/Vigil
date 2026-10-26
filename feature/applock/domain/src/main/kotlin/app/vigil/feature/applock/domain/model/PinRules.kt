package app.vigil.feature.applock.domain.model

/** What makes a valid PIN: exactly [LENGTH] digits (docs/design/README.md, flow 1). */
public object PinRules {
    public const val LENGTH: Int = 6

    public fun isValid(pin: String): Boolean = pin.length == LENGTH && pin.all(Char::isDigit)
}
