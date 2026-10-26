# App lock: PIN + biometrics over the whole app

Rules: `.claude/rules/app.md` (layer order), `.claude/rules/datastore.md` (the store). Once
`:feature:applock` exists, its source files win over this sample.

## Behaviour

- Locked on every process start and after **30 s or more** in the background (process lifecycle,
  so a rotation is not a trip to the background). Locked only once a PIN exists.
- PIN: exactly 6 digits (`PinRules.LENGTH` in `domain`). Created right after the first login
  (`CreatePinRoute`, design 1.1–1.3) in three steps (`CreatePinStep`): **Enter** → **Confirm** →
  **Biometric**. Confirm shows "PINs match" / "PINs don't match" once all digits are in (`PinMatch`);
  after a mismatch the next digit starts a new attempt. Biometric (only with BIOMETRIC_STRONG
  available, otherwise Confirm saves) explains fingerprint unlock: "Enable fingerprint" shows
  `BiometricPrompt` and saves with biometrics on after a success; "Not now" saves without. Back goes
  one step back; on Enter it calls `onBackToSignIn`.
- PIN entry uses the app's own **number pad** (`components/PinPad.kt`), never the system keyboard:
  `PinDots` (one dot per digit, `PinDotsStatus` Typing / Error (shakes once) / Success; read out
  only as "PIN, n of 6 digits entered", a polite live region) and `PinPad` (76 dp round keys 1–9, 0
  and delete, pressed keys in `primaryContainer`; an optional fingerprint key left of 0). Unlock
  checks the PIN as soon as all 6 digits are in; onboarding uses Continue.
- The security screens use `SecurityTheme` from `:common:ui` (dark scheme, light system bar icons).
- Stored as PBKDF2-HMAC-SHA256 (random 16-byte salt, 120 000 iterations) in the encrypted
  `applock_settings.json`; never the PIN itself, never in `savedStateHandle`. Compared in constant
  time (`MessageDigest.isEqual`). A short PIN can't resist offline brute force on its own; the real
  protection is the Keystore-encrypted store.
- The lock state is unknown at first: the app stays **covered** until it's known (fail closed). If
  the store can't be read, the lock screen shows with a Retry snackbar.
- The lock screen is opaque, the content underneath stays composed (navigation state survives) but
  is removed from the semantics tree, and Back sends the app to the background.
- Logout resets the store (`UserDataCleaner`), so the next user creates their own PIN.
- TODO in the first version: limit wrong attempts (growing delay, then log out).

## Layers

```
:feature:applock:data:api   AppLockRepository (observeLockState, createPin, unlockWithPin,
                            unlockWithBiometric, onAppBackgrounded/Foregrounded, isBiometricAvailable)
:feature:applock:data:core  AppLockSettings (store), PinHasher, AppLockSession (@Singleton, in memory,
                            injected kotlin.time.TimeSource), BiometricAvailability (BiometricManager)
:feature:applock:domain     PinRules, AppLockState, SAM use cases (Observe…, CreatePin, UnlockWithPin,
                            UnlockWithBiometric, MarkAppBackgrounded/Foregrounded, IsBiometricAvailable)
:feature:applock:ui         AppLockHost (public), AppLockViewModel, AppLockScreen,
                            BiometricPromptHandler, CreatePinRoute (public)
```

## The session (data:core)

```kotlin
@Singleton
internal class AppLockSession @Inject constructor(private val timeSource: TimeSource) {
    private val locked = MutableStateFlow(true)               // a new process starts locked
    private var backgroundedAt: TimeMark? = null              // kotlin.time.TimeMark

    val isLocked: StateFlow<Boolean> = locked.asStateFlow()

    fun unlock() { locked.value = false }
    fun onBackgrounded() { backgroundedAt = timeSource.markNow() }
    fun onForegrounded() {
        val mark = backgroundedAt ?: return
        backgroundedAt = null
        if (mark.elapsedNow() >= GRACE_PERIOD) locked.value = true
    }

    companion object { val GRACE_PERIOD = 30.seconds }
}

// repository: locked only when a PIN exists; createPin and a matching PIN unlock the session
override fun observeLockState() = combine(store.data.asStoreResult(), session.isLocked) { settings, locked ->
    settings.map { AppLockStateData(isLocked = locked && it.hasPin, isBiometricEnabled = it.biometricEnabled && biometric.isAvailable()) }
}.distinctUntilChanged()
```

Test the session with `kotlin.time.TestTimeSource`, the repository with a real encrypted store on a
temp file (see `reference/datastore.md`).

## The host (ui)

```kotlin
@Composable
fun AppLockHost(content: @Composable () -> Unit) {
    val viewModel: AppLockViewModel = hiltViewModel()          // activity-scoped: above navigation
    val processLifecycle = ProcessLifecycleOwner.get()          // androidx.lifecycle:lifecycle-process
    LifecycleEventEffect(Lifecycle.Event.ON_STOP, processLifecycle) { viewModel.onAction(AppLockAction.AppBackgrounded) }
    LifecycleEventEffect(Lifecycle.Event.ON_START, processLifecycle) { viewModel.onAction(AppLockAction.AppForegrounded) }
    AppLockLayer(state = viewModel.uiState(), onAction = viewModel::onAction, content = content)
}

@Composable
internal fun AppLockLayer(state: AppLockUiState, onAction: (AppLockAction) -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val covered = state.status != AppLockStatus.Unlocked      // Loading counts as covered
    Box(modifier.fillMaxSize()) {
        Box(if (covered) Modifier.clearAndSetSemantics {} else Modifier) { content() }
        if (covered) {
            val activity = LocalActivity.current
            BackHandler { activity?.moveTaskToBack(true) }     // after the content, so it wins
            AppLockScreen(state = state, onAction = onAction, modifier = Modifier.fillMaxSize())
        }
    }
}
```

## Biometric prompt as consumable state

The ViewModel sets `biometricRequest = BiometricRequest()` (a plain class: identity) when the app
locks with biometrics enabled, or on "Use biometrics". `BiometricPromptHandler` consumes it before
showing the prompt, like `NavigationHandler`:

```kotlin
LaunchedEffect(request) {
    request ?: return@LaunchedEffect
    currentOnRequestConsumed(request)
    val activity = activity ?: return@LaunchedEffect           // LocalActivity.current as? FragmentActivity
    BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = currentOnSucceeded()
    }).authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(usePin)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build(),
    )
}
```

`androidx.biometric` 1.1.0 is the latest stable (1.4 is alpha). `MainActivity` must be a
`FragmentActivity`.

## In `:app`

```kotlin
val backStack = rememberNavBackStack(LoginKey)
AppLockHost {
    AuthSessionHost(onSessionExpired = { backStack.replaceAllWith(LoginKey) }) {
        NavDisplay(
            ...,
            entryProvider = entryProvider {
                entry<LoginKey> { LoginRoute(onLoggedIn = { backStack.replaceAllWith(CreatePinKey) }) }
                entry<CreatePinKey> { CreatePinRoute(onPinCreated = { backStack.replaceAllWith(MainKey) }) }
                entry<MainKey> { MainTabs(…) }
            },
        )
    }
}
```
