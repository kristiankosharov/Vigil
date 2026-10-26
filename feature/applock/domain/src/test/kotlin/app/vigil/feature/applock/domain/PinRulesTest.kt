package app.vigil.feature.applock.domain

import app.vigil.feature.applock.domain.model.PinRules
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PinRulesTest : FunSpec({
    test("exactly 6 digits are valid") {
        PinRules.isValid("123456") shouldBe true
    }

    test("too short, too long or non-digits are invalid") {
        listOf("", "12345", "1234567", "12a456", "123 56").forEach { PinRules.isValid(it) shouldBe false }
    }

    test("repeats, runs, well-known PINs and dates are weak") {
        listOf("000000", "777777", "123456", "654321", "890123", "123123", "250390", "122590", "900325")
            .forEach { PinRules.isWeak(it.toCharArray()) shouldBe true }
    }

    test("other PINs are fine") {
        listOf("482916", "739058", "360471").forEach { PinRules.isWeak(it.toCharArray()) shouldBe false }
    }
})
