# `:app`: navigation, environment, logging setup

Rules: `.claude/rules/app.md`, `.claude/rules/build.md`.

## Navigation 3

```kotlin
@Serializable data object UsersKey : NavKey
@Serializable data class UserDetailsKey(val userId: String) : NavKey

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(UsersKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<UsersKey> { UsersRoute(onNavigateToDetails = { backStack.add(UserDetailsKey(it)) }) }
            entry<UserDetailsKey> { key -> UserDetailsRoute(userId = key.userId, onBack = { backStack.removeLastOrNull() }) }
        },
    )
}
```

## Flavors, BuildConfig and `EnvironmentModule`

```kotlin
// build-logic: vigil.environment (applied only to :app)
android {
    flavorDimensions += "environment"
    productFlavors {
        create("dev") { dimension = "environment"; applicationIdSuffix = ".dev" }
        create("staging") { dimension = "environment"; applicationIdSuffix = ".staging" }
        create("prod") { dimension = "environment" }
    }
}

// :app/build.gradle.kts
android {
    buildFeatures { buildConfig = true }
    productFlavors {
        getByName("dev") { buildConfigField("String", "BASE_URL", "\"https://dev.api.example.com/\"") }
        getByName("staging") { buildConfigField("String", "BASE_URL", "\"https://staging.api.example.com/\"") }
        getByName("prod") { buildConfigField("String", "BASE_URL", "\"https://api.example.com/\"") }
    }

    // Release signing only when CI provides the keystore. Without it the release build is
    // unsigned but still runs R8, so scripts/check.sh release works locally.
    val keystorePath = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").get()
            }
        }
    }
    buildTypes {
        release { signingConfig = signingConfigs.findByName("release") }
    }
}

// :common:network — qualifiers only, no BuildConfig
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class BaseUrl
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IsDebugBuild

// :app — di/EnvironmentModule.kt
@Module
@InstallIn(SingletonComponent::class)
object EnvironmentModule {
    @Provides @BaseUrl
    fun provideBaseUrl(): String = BuildConfig.BASE_URL

    @Provides @IsDebugBuild
    fun provideIsDebugBuild(): Boolean = BuildConfig.DEBUG
}
```

## Timber setup

```kotlin
// :app
@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())
    }
}

private class ReleaseTree : Timber.Tree() {
    override fun isLoggable(tag: String?, priority: Int) = priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // forward to the crash reporter when one is added
    }
}
```
