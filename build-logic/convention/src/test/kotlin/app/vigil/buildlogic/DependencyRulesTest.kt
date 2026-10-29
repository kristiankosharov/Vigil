package app.vigil.buildlogic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class DependencyRulesTest : FunSpec({

    test("allowed dependencies") {
        listOf(
            ":app" to ":feature:home:ui",
            ":feature:home:ui" to ":feature:home:domain",
            ":feature:home:ui" to ":common:ui",
            ":feature:home:ui" to ":common:core",
            ":feature:home:domain" to ":feature:home:data:api",
            ":feature:home:domain" to ":feature:auth:data:api",
            ":feature:home:data:api" to ":common:core",
            ":feature:home:data:core" to ":feature:home:data:api",
            ":feature:home:data:core" to ":feature:auth:data:api",
            ":feature:home:data:core" to ":common:security",
            ":common:network" to ":common:core",
            ":feature:home:ui" to ":common:testing",
            ":app" to ":app",
        ).forEach { (from, to) ->
            withClue(from, to) { dependencyRuleViolation(from, to).shouldBeNull() }
        }
    }

    test("runtimeOnly lets a ui bring its own data:core for Hilt, and nothing else") {
        dependencyRuleViolation(":feature:home:ui", ":feature:home:data:core", runtimeOnly = true).shouldBeNull()
        listOf(
            ":feature:home:ui" to ":feature:auth:data:core",
            ":feature:home:ui" to ":feature:home:data:api",
            ":feature:home:domain" to ":feature:home:data:core",
            ":app" to ":feature:home:data:core",
        ).forEach { (from, to) ->
            withClue(from, to) { dependencyRuleViolation(from, to, runtimeOnly = true).shouldNotBeNull() }
        }
    }

    test("forbidden dependencies") {
        listOf(
            ":feature:home:ui" to ":feature:home:data:api",
            ":feature:home:ui" to ":feature:home:data:core", // allowed only as runtimeOnly
            ":feature:home:ui" to ":common:network",
            ":feature:home:ui" to ":feature:auth:ui",
            ":feature:home:ui" to ":feature:auth:domain",
            ":feature:home:domain" to ":feature:home:data:core",
            ":feature:home:domain" to ":feature:auth:domain",
            ":feature:home:ui" to ":common:security",
            ":feature:home:domain" to ":common:security",
            ":feature:home:data:api" to ":feature:home:domain",
            ":feature:home:data:core" to ":feature:auth:data:core",
            ":common:ui" to ":feature:home:ui",
            ":feature:home:ui" to ":app",
            ":app" to ":feature:home:data:core",
            ":app" to ":feature:home:data:api",
            ":app" to ":feature:home:domain",
        ).forEach { (from, to) ->
            withClue(from, to) { dependencyRuleViolation(from, to).shouldNotBeNull() }
        }
    }
})

private inline fun withClue(from: String, to: String, block: () -> Unit) =
    io.kotest.assertions.withClue("$from → $to") { block() }
