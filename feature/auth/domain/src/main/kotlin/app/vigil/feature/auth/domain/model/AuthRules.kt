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

    // A number as people type it: an optional "+", then digits with spaces, brackets or dashes.
    private val PhoneLikePattern = Regex("^\\+?\\d[\\d ()-]*$")

    /**
     * Which sign-in method a single "email or phone number" field is heading for: anything with an
     * "@" is an email, a typed number is a phone; anything else (or nothing) is [SignInIdentifierKind.Unknown].
     */
    public fun identifierKind(value: String): SignInIdentifierKind {
        val trimmed = value.trim()
        return when {
            '@' in trimmed -> SignInIdentifierKind.Email
            PhoneLikePattern.matches(trimmed) -> SignInIdentifierKind.Phone
            else -> SignInIdentifierKind.Unknown
        }
    }

    /** Keeps only "+" and digits, the form Firebase expects: "+359 888 123 456" → "+359888123456". */
    public fun normalizePhoneNumber(value: String): String = value.filter { it == '+' || it.isDigit() }

    public fun isValidEmail(email: String): Boolean = EmailPattern.matches(email)

    public fun isValidPassword(password: String): Boolean = password.length >= PASSWORD_MIN_LENGTH

    public fun isValidPhoneNumber(phoneNumber: String): Boolean = PhonePattern.matches(phoneNumber)

    public fun isValidPhoneCode(code: String): Boolean = code.length == PHONE_CODE_LENGTH && code.all(Char::isDigit)
}
