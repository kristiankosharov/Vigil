package app.vigil.feature.auth.domain.model

/** Input checks done before calling Firebase, which enforces the same rules. */
public object AuthRules {
    /** Firebase's own minimum. */
    public const val PASSWORD_MIN_LENGTH: Int = 6

    public const val PHONE_CODE_LENGTH: Int = 6

    // Deliberately loose: Firebase validates the address; this only catches typos.
    private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    // E.164: "+", country code, up to 15 digits in all.
    private val PhonePattern = Regex("^\\+[1-9]\\d{6,14}$")

    public fun isValidEmail(email: String): Boolean = EmailPattern.matches(email)

    public fun isValidPassword(password: String): Boolean = password.length >= PASSWORD_MIN_LENGTH

    public fun isValidPhoneNumber(phoneNumber: String): Boolean = PhonePattern.matches(phoneNumber)

    public fun isValidPhoneCode(code: String): Boolean = code.length == PHONE_CODE_LENGTH && code.all(Char::isDigit)
}
