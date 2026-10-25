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

    test("SMS codes are six digits") {
        AuthRules.isValidPhoneCode("123456") shouldBe true
        AuthRules.isValidPhoneCode("12345") shouldBe false
        AuthRules.isValidPhoneCode("12345a") shouldBe false
    }
})
