package app.vigil.feature.applock.domain.model

/** What makes a valid PIN: exactly [LENGTH] digits (docs/design/README.md, flow 1), and not an easy guess. */
public object PinRules {
    public const val LENGTH: Int = 6

    private const val DIGITS = 10
    private const val MAX_DAY = 31
    private const val MAX_MONTH = 12

    /** Steps between neighbouring digits (mod 10) that make a PIN a repeat or a run: 0, +1, -1. */
    private val RunSteps = setOf(0, 1, DIGITS - 1)

    // Among the most used 6-digit PINs that the patterns below don't already catch.
    private val Common = setOf("123123", "121212", "112233", "123321", "131313", "102030", "159753", "147258", "789456")

    public fun isValid(pin: String): Boolean = pin.length == LENGTH && pin.all(Char::isDigit)

    /**
     * Easy to guess: one digit repeated (000000), a run up or down (123456, 987654, also wrapping
     * like 890123), a well-known PIN, or something that reads as a date (DDMMYY, MMDDYY, YYMMDD).
     */
    public fun isWeak(pin: CharArray): Boolean {
        val digits = pin.map(Char::digitToInt) // never a String: the PIN must stay wipeable
        val steps = digits.zipWithNext { a, b -> (b - a + DIGITS) % DIGITS }.toSet()
        return (steps.size == 1 && steps.first() in RunSteps) ||
            Common.any { it.length == pin.size && it.indices.all { i -> it[i] == pin[i] } } ||
            looksLikeDate(digits)
    }

    private fun looksLikeDate(digits: List<Int>): Boolean {
        val (a, b, c) = digits.chunked(2).map { (tens, ones) -> tens * DIGITS + ones }
        fun isDay(n: Int) = n in 1..MAX_DAY
        fun isMonth(n: Int) = n in 1..MAX_MONTH
        return (isDay(a) && isMonth(b)) || (isMonth(a) && isDay(b)) || (isMonth(b) && isDay(c))
    }
}
