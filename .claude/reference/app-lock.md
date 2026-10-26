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
- **The PIN is never stored, not even as a hash.** It unwraps the **vault key** (`:common:security`):
  - `pinKey = HMAC(Keystore pepper, Argon2id(PIN, salt))` (`PinKeyDerivation`; Argon2id from Bouncy
    Castle, OWASP baseline 19 MiB / 2 passes / 1 lane, stored per wrapped key as `KdfParams`). The
    pepper is a non-exportable HMAC key (StrongBox if present, else TEE), so a guess can only be
    checked on this device; copied files can't be brute-forced elsewhere.
  - `AppLockSettings.pinKey` holds `AES-GCM(pinKey, vaultKey)` (associated data
    `vigil.vault-key.pin`). A PIN is right exactly when that opens (no `AEADBadTagException`).
  - The vault key is random (AES-256), lives in memory only while unlocked (`Vault`, opened and closed
    by `AppLockSession`), and seals other features' secrets (the backend tokens). A new PIN while
    unlocked re-wraps the same key.
- **Fingerprint** wraps a second copy with a Keystore AES-GCM key that needs a strong biometric for
  every use and is destroyed when fingerprints are added or removed (`BiometricKeys`). BiometricPrompt
  always gets a `CryptoObject`: enrolment encrypts the vault key with the authenticated cipher, unlock
  decrypts it. An invalidated key turns fingerprint unlock off (`prepareBiometricUnlock` → null).
- **Wrong PINs** (`PinAttemptPolicy`): 5 free tries, then 30 s, 1 min, 5 min, 15 min; the 10th wrong
  PIN erases everything (`Set<UserDataCleaner>`, the biometric key) and signs out
  (`SessionEvents.notifyExpired()`). The count is written **before** each check; waits use time
  since boot (`DeviceClock`), and after a reboot the full wait applies again. One check at a time
  (`Mutex`). The lock screen counts the wait down and warns from 3 attempts left.
- **Easy PINs are refused** on Create (`PinRules.isWeak`): one repeated digit, runs up/down (wrapping),
  well-known PINs, and anything that reads as DDMMYY / MMDDYY / YYMMDD.
- **PINs in memory** are `CharArray`s (`PinBuffer` in the UI, wiped after each check or save and when
  the lock screen goes to the background), never `String`s and never `savedStateHandle`. The UI
  state carries only the length.
- The whole app has `FLAG_SECURE` (`MainActivity`).
- The lock state is unknown at first: the app stays **covered** until it's known (fail closed). If
  the store can't be read, the lock screen shows with a Retry snackbar.
- The lock screen is opaque, the content underneath stays composed (navigation state survives) but
  is removed from the semantics tree, and Back sends the app to the background.
- Logout resets the store (`UserDataCleaner`), so the next user creates their own PIN.

## Layers

```
:common:security            Vault + VaultKeyControl (VaultImpl, @Singleton), AesGcm, PinKeyDerivation
                            (Argon2id + DevicePepper), BiometricKeys
:feature:applock:data:api   AppLockRepository (observeLockState, createPin, unlockWithPin → PinCheckData,
                            prepareBiometricEnrollment/Unlock, unlockWithBiometric, lockNow,
                            onAppBackgrounded/Foregrounded, isBiometricAvailable), BiometricCipherData
:feature:applock:data:core  AppLockSettings (store: wrapped keys, attempt count, lockout), PinAttemptPolicy,
                            AppLockSession (@Singleton, opens/closes the vault), DeviceClock,
                            BiometricAvailability (BiometricManager)
:feature:applock:domain     PinRules (isValid, isWeak), AppLockState, PinCheck, BiometricCipher, SAM use
                            cases (Observe…, CreatePin, UnlockWithPin, PrepareBiometricEnrollment/Unlock,
                            UnlockWithBiometric, MarkAppBackgrounded/Foregrounded, IsBiometricAvailable)
:feature:applock:ui         AppLockHost (public), AppLockViewModel, AppLockScreen, PinBuffer,
                            BiometricPromptHandler (CryptoObject), CreatePinRoute (public)
```

## The session (data:core)

```kotlin
@Singleton
internal class AppLockSession @Inject constructor(private val timeSource: TimeSource) {
    private val locked = MutableStateFlow(true)               // a new process starts locked
    private var backgroundedAt: TimeMark? = null              // kotlin.time.TimeMark

    val isLocked: StateFlow<Boolean> = locked.asStateFlow()

    fun unlock(vaultKey: ByteArray) { vault.open(vaultKey); locked.value = false }   // vault: VaultKeyControl
    fun lock() { vault.close(); locked.value = true }
    fun onBackgrounded() { backgroundedAt = timeSource.markNow() }
    fun onForegrounded() {
        val mark = backgroundedAt ?: return
        backgroundedAt = null
        if (mark.elapsedNow() >= GRACE_PERIOD) lock()
    }

    companion object { val GRACE_PERIOD = 30.seconds }
}

// repository: locked only when a PIN exists; createPin, a right PIN or a fingerprint open the vault
override fun observeLockState() = combine(store.data.asStoreResult(), session.isLocked) { settings, locked ->
    settings.map { AppLockStateData(isLocked = locked && it.hasPin, isBiometricEnabled = it.biometricKey != null && biometric.isAvailable(), retryAfter = it.remainingLockout()) }
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

The ViewModel asks the repository for a Keystore cipher (`prepareBiometricUnlock` /
`prepareBiometricEnrollment`) and sets `biometricRequest = BiometricRequest(cipher)` (a plain class:
identity) when the app locks with biometrics enabled, or on "Use biometrics". `BiometricPromptHandler`
consumes it before showing the prompt, like `NavigationHandler`; the ViewModel keeps the consumed
request's cipher and, on success, passes it back (`unlockWithBiometric(cipher)` / `createPin(pin, cipher)`):

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
        BiometricPrompt.CryptoObject(request.cipher.cipher),       // never a prompt without a CryptoObject
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
