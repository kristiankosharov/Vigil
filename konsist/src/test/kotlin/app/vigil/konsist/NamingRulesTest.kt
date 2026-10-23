package app.vigil.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.FunSpec

class NamingRulesTest : FunSpec({
    val scope = Konsist.scopeFromProduction()

    test("repository implementations are named <Interface>Impl") {
        scope.classes()
            .filter { cls -> cls.parents().any { it.name.endsWith("Repository") } }
            .assertTrue { it.name.endsWith("RepositoryImpl") && it.hasParentWithName(it.name.removeSuffix("Impl")) }
    }

    test("repository interfaces end with Repository and live in data:api") {
        scope.interfaces()
            .withNameEndingWith("Repository")
            .assertTrue { it.resideInPackage("..data..") && it.path.contains("/data/api/") }
    }

    test("use case implementations are named <Interface>Impl") {
        scope.classes()
            .filter { cls -> cls.parents().any { it.name.endsWith("UseCase") } }
            .assertTrue { it.name.endsWith("UseCaseImpl") && it.hasParentWithName(it.name.removeSuffix("Impl")) }
    }

    test("use case interfaces end with UseCase and live in domain") {
        scope.interfaces()
            .withNameEndingWith("UseCase")
            .assertTrue { it.resideInPackage("..domain..") }
    }

    test("implementations are never named DefaultX, RealX or XImplementation") {
        val forbidden = Regex("""^(Default|Real)[A-Z].*|.*Implementation$""")
        scope.classes().assertFalse { forbidden.matches(it.name) }
    }

    test("Impl classes are internal") {
        scope.classes().withNameEndingWith("Impl").assertTrue { it.hasInternalModifier }
    }
})
