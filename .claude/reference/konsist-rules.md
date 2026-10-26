# Konsist architecture rules

Source for the `:konsist` module. Once that module exists, it is the truth; keep this file in sync or delete it. Adjust Konsist API calls to the installed version if a name differs; keep the rule.

Konsist checks source code only. Module dependencies are checked by `enforceDependencyRules()` in
`build-logic`, and `!!`, `GlobalScope`, `runCatching` in suspend code and hardcoded dispatchers by
detekt (`.claude/reference/build-logic.md`).

```kotlin
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

class LayerRulesTest : FunSpec({
    val scope = Konsist.scopeFromProduction()

    test("ui modules do not import data packages") {
        scope.files
            .filter { it.path.contains("/ui/src/main/") && it.path.contains("/feature/") }
            .assertFalse { file -> file.imports.any { it.name.contains(".data.") } }
    }

    test("Hilt modules live in a di package and end with Module") {
        scope.classesAndInterfacesAndObjects()
            .filter { it.hasAnnotationWithName("Module") }
            .assertTrue { it.resideInPackage("..di..") && it.name.endsWith("Module") }
    }

    test("one Hilt module per file") {
        scope.files.assertFalse { file ->
            file.classesAndInterfacesAndObjects().count { it.hasAnnotationWithName("Module") } > 1
        }
    }

    test("a Hilt module has either @Binds or @Provides, never both") {
        scope.classesAndInterfacesAndObjects()
            .filter { it.hasAnnotationWithName("Module") }
            .assertFalse { module ->
                val functions = module.functions()
                functions.any { it.hasAnnotationWithName("Binds") } &&
                    functions.any { it.hasAnnotationWithName("Provides") }
            }
    }

    test("feature ui modules expose only *Route and *Host composables") {
        val inUi = { path: String -> "/feature/" in path && "/ui/src/main/" in path }
        scope.functions()
            .filter { it.isTopLevel && inUi(it.path) }
            .assertTrue { it.hasInternalModifier || it.hasPrivateModifier || it.name.endsWith("Route") || it.name.endsWith("Host") }
        scope.classesAndInterfacesAndObjects()
            .filter { it.isTopLevel && inUi(it.path) }
            .assertTrue { it.hasInternalModifier || it.hasPrivateModifier }
    }

    test("ViewModels are named *ViewModel, extend BaseViewModel and expose @Composable uiState()") {
        scope.classes()
            .filter { it.hasAnnotationWithName("HiltViewModel") }
            .assertTrue { vm ->
                vm.name.endsWith("ViewModel") &&
                    vm.parents().any { it.name.substringBefore("<") == "BaseViewModel" } &&
                    vm.functions().any { it.name == "uiState" && it.hasAnnotationWithName("Composable") }
            }
    }

    test("UiEffectQueue is only used by BaseViewModel") {
        scope.files
            .filter { it.name != "BaseViewModel" && it.name != "UiEffectQueue" }
            .assertFalse { it.text.contains("UiEffectQueue") || it.text.contains("mutableStateListOf<UiEffect") }
    }

    test("ViewModels do not use StateFlow for UI state") {
        scope.classes()
            .withNameEndingWith("ViewModel")
            .assertFalse { it.text.contains("MutableStateFlow") }
    }

    test("toasts and snackbars are shown only by UiEffectHandler in common:ui") {
        scope.files
            .filter { "/common/ui/" !in it.path }
            .assertFalse { it.text.contains("Toast.makeText") || it.text.contains(".showSnackbar(") }
    }

    test("ViewModels do not use Channel or SharedFlow for events") {
        scope.classes()
            .withNameEndingWith("ViewModel")
            .assertFalse { it.text.contains("Channel<") || it.text.contains("MutableSharedFlow") }
    }

    test("Route composables contain no side effects or logic") {
        scope.functions()
            .withNameEndingWith("Route")
            .assertFalse { route ->
                listOf("LaunchedEffect", "DisposableEffect", "SideEffect", "remember", "if (", "when (", "when {")
                    .any { route.text.contains(it) }
            }
    }

    test("ViewModels do not depend on repositories, APIs or DAOs") {
        scope.classes()
            .withNameEndingWith("ViewModel")
            .assertFalse { vm ->
                vm.primaryConstructor?.parameters.orEmpty().any { param ->
                    val type = param.type.name
                    type.endsWith("Repository") || type.endsWith("Api") || type.endsWith("Dao")
                }
            }
    }

    test("everything in data:core is internal") {
        scope.classesAndInterfacesAndObjects()
            .filter { "/data/core/src/main/" in it.path && it.isTopLevel }
            .assertTrue { it.hasInternalModifier }
    }

    test("no Room entities, DAOs or databases while :common:database is gone") {
        scope.classesAndInterfaces()
            .assertFalse { it.hasAnnotationWithName("Entity", "Dao", "Database") }
    }

    test("DataStores are typed and encrypted; no SharedPreferences") {
        scope.files
            .filter { "/common/datastore/" !in it.path }
            .assertFalse { file ->
                listOf(
                    "preferencesDataStore", "PreferenceDataStoreFactory", "DataStoreFactory",
                    "DataStore.Builder", "AeadSerializer", "getSharedPreferences", "SharedPreferences",
                ).any { file.text.contains(it) }
            }
    }

    test("Actions, effects and navigation contain no function types") {
        scope.classesAndObjects()
            .filter { cls ->
                cls.parents().any { p ->
                    val name = p.name.substringBefore("<") // Konsist parent names include type arguments
                    name.endsWith("Action") || name == "UiEffect" || name.endsWith("Navigation")
                }
            }
            .assertFalse { cls -> cls.properties().any { it.type?.text?.contains("->") == true } }
    }

    test("log only through Timber: no Log.x calls, println or printStackTrace") {
        // Log.WARN etc. constants are allowed (used by ReleaseTree); Log.d/i/w/e/v/wtf calls are not.
        val logCall = Regex("""\bLog\.(v|d|i|w|e|wtf)\(""")
        scope.files.assertFalse { file ->
            logCall.containsMatchIn(file.text) ||
                file.text.contains("println(") ||
                file.text.contains(".printStackTrace()")
        }
    }

    test("DebugTree is planted only in debug") {
        scope.files.assertFalse { file ->
            "Timber.plant(Timber.DebugTree())" in file.text && "BuildConfig.DEBUG" !in file.text
        }
    }

    test("only :app reads BuildConfig") {
        scope.files
            .filter { !it.path.contains("/app/src/") }
            .assertFalse { it.text.contains("BuildConfig.") }
    }
})
```
