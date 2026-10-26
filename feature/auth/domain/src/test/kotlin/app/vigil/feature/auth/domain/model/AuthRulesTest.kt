package app.vigil.feature.auth.domain.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AuthRulesTest : FunSpec({
    test("emails") {
        AuthRules.isValidEmail("ana@example.com") shouldBe true
        AuthRules.isValidEmail("ana@example") shouldBe false
        AuthRules.isValidEmail("ana example@x.com") shouldBe false
    }

    test("passwords need Firebase's minimum length") {
        AuthRules.isValidPassword("12345") shouldBe false
        AuthRules.isValidPassword("123456") shouldBe true
    }

    test("phone numbers are E.164") {
        AuthRules.isValidPhoneNumber("+359888123456") shouldBe true
        AuthRules.isValidPhoneNumber("0888123456") shouldBe false
        AuthRules.isValidPhoneNumber("+0888123456") shouldBe false
        AuthRules.isValidPhoneNumber("+1234567890123456") shouldBe false
    }

    test("the sign-in field is an email with an @, a phone when it is a typed number") {
        AuthRules.identifierKind("ana@example.com") shouldBe SignInIdentifierKind.Email
        AuthRules.identifierKind(" ana@ ") shouldBe SignInIdentifierKind.Email // heading for an email already
        AuthRules.identifierKind("+359 888 123 456") shouldBe SignInIdentifierKind.Phone
        AuthRules.identifierKind("0888-123-456") shouldBe SignInIdentifierKind.Phone
        AuthRules.identifierKind("+1 (555) 010 9999") shouldBe SignInIdentifierKind.Phone
        AuthRules.identifierKind("ana") shouldBe SignInIdentifierKind.Unknown
        AuthRules.identifierKind("+") shouldBe SignInIdentifierKind.Unknown
        AuthRules.identifierKind("   ") shouldBe SignInIdentifierKind.Unknown
    }

    test("phone numbers are normalized to + and digits") {
        AuthRules.normalizePhoneNumber("+359 888 123 456") shouldBe "+359888123456"
        AuthRules.normalizePhoneNumber("+1 (555) 010-9999") shouldBe "+15550109999"
    }

    test("SMS codes are six digits") {
        AuthRules.isValidPhoneCode("123456") shouldBe true
        AuthRules.isValidPhoneCode("12345") shouldBe false
        AuthRules.isValidPhoneCode("12345a") shouldBe false
    }
})
