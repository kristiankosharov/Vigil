package app.vigil.feature.applock.domain

import app.vigil.feature.applock.domain.model.PinRules
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PinRulesTest : FunSpec({
    test("4 to 8 digits are valid") {
        listOf("1234", "12345678").forEach { PinRules.isValid(it) shouldBe true }
    }

    test("too short, too long or non-digits are invalid") {
        listOf("", "123", "123456789", "12a4", "12 34").forEach { PinRules.isValid(it) shouldBe false }
    }
})
