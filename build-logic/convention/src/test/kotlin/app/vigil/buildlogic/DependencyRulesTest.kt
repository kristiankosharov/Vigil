package app.vigil.buildlogic

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class DependencyRulesTest : FunSpec({

    test("allowed dependencies") {
        listOf(
            ":app" to ":feature:home:ui",
            ":app" to ":feature:home:data:core",
            ":feature:home:ui" to ":feature:home:domain",
            ":feature:home:ui" to ":common:ui",
            ":feature:home:ui" to ":common:core",
            ":feature:home:domain" to ":feature:home:data:api",
            ":feature:home:domain" to ":feature:auth:data:api",
            ":feature:home:data:api" to ":common:core",
            ":feature:home:data:core" to ":feature:home:data:api",
            ":feature:home:data:core" to ":feature:auth:data:api",
            ":feature:home:data:core" to ":common:database",
            ":common:network" to ":common:core",
            ":feature:home:ui" to ":common:testing",
        ).forEach { (from, to) ->
            withClue(from, to) { dependencyRuleViolation(from, to).shouldBeNull() }
        }
    }

    test("forbidden dependencies") {
        listOf(
            ":feature:home:ui" to ":feature:home:data:api",
            ":feature:home:ui" to ":feature:home:data:core",
            ":feature:home:ui" to ":common:network",
            ":feature:home:ui" to ":feature:auth:ui",
            ":feature:home:ui" to ":feature:auth:domain",
            ":feature:home:domain" to ":feature:home:data:core",
            ":feature:home:domain" to ":feature:auth:domain",
            ":feature:home:domain" to ":common:database",
            ":feature:home:data:api" to ":feature:home:domain",
            ":feature:home:data:core" to ":feature:auth:data:core",
            ":common:ui" to ":feature:home:ui",
            ":feature:home:ui" to ":app",
        ).forEach { (from, to) ->
            withClue(from, to) { dependencyRuleViolation(from, to).shouldNotBeNull() }
        }
    }
})

private inline fun withClue(from: String, to: String, block: () -> Unit) =
    io.kotest.assertions.withClue("$from → $to") { block() }
