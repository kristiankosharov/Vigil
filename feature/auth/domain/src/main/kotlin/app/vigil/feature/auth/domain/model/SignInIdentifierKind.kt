package app.vigil.feature.auth.domain.model

/** What the single "email or phone number" sign-in field holds; see [AuthRules.identifierKind]. */
public enum class SignInIdentifierKind { Email, Phone, Unknown }
