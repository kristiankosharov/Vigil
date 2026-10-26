---
paths:
  - "common/network/**"
  - "feature/auth/**"
  - "feature/*/data/core/**/remote/**"
---

# Network and authentication

Code: `.claude/reference/network-auth.md`.

## HttpClients (`:common:network`)

- Two `@Singleton` clients:
  - `@Authenticated HttpClient` — default for feature APIs, has the `Auth` plugin.
  - `@Unauthenticated HttpClient` — no `Auth` plugin; used only by login, register and
    `TokenRefresher`, so a refresh can never trigger another refresh.
- Both use `ContentNegotiation` + `Json { ignoreUnknownKeys = true; explicitNulls = false }`,
  `HttpTimeout`, `DefaultRequest` with the injected `@BaseUrl`, and **`expectSuccess = true`** so
  non-2xx responses throw `ResponseException` and are mapped by `safeApiCall`.
- Ktor `Logging` only when the injected `@IsDebugBuild` is true; it logs through Timber with tag
  `Ktor` and uses `sanitizeHeader { it == HttpHeaders.Authorization }`.
- `@BaseUrl` and `@IsDebugBuild` are qualifiers declared here and provided by `:app`. This module
  never reads `BuildConfig`.
- Features never create an `HttpClient`; they inject `@Authenticated` into their internal `*ApiImpl`.
- Every call goes through `safeApiCall`.

## Sign-in (Firebase Auth)

- Sign-in goes through Firebase Auth, only in `:feature:auth:data:core` (`AuthRepositoryImpl`):
  email/password (sign in, sign up, password reset), phone (SMS code) and OpenID Connect
  (`OAuthProvider`, providers listed in `remote/OidcProviders.kt`, empty until one is set up).
- Every Firebase call goes through the feature's `safeFirebaseCall`, which maps failures to
  `AppError.Auth.*` (`InvalidCredentials`, `AccountExists`, `WeakPassword`, `InvalidPhoneNumber`,
  `InvalidCode`, `TooManyRequests`, `Cancelled`, `Unknown`) or `AppError.Network.NoConnection`, and
  logs them without the throwable (Firebase messages can name the account).
- Phone verification is a `callbackFlow`: `CodeSent(verificationId)`, then `Verified` if the device
  verifies by itself. Firebase SDK types never leave `data:core`.
- Flows that need an `Activity` (phone reCAPTCHA fallback, OpenID Connect) get the resumed one from
  `CurrentActivityTracker`, held weakly; no activity → `AppError.Auth.Unknown`, never a crash.
- Logout signs out of Firebase in the auth feature's `UserDataCleaner`.
- The backend's bearer tokens below are not yet issued: exchanging the Firebase ID token for them
  after sign-in is `TODO(auth)` in `AuthRepositoryImpl`.

## Authentication (Ktor `Auth`, bearer tokens)

- `:common:network` owns the mechanism and depends only on interfaces: `TokenStorage` and
  `TokenRefresher` (implemented in `:feature:auth:data:core`) and `SessionEvents` (implemented here).
- `refreshTokens`: no refresh token → session expired. On refresh failure, log out (clear tokens,
  notify expiry) **only** if the server rejected the refresh token (HTTP 400/401), never on network
  errors.
- Ktor's bearer provider already makes concurrent 401s share one refresh. Never add your own refresh
  lock or retry loop.
- `TokenStorage` uses the auth feature's own encrypted DataStore (`auth_session.json`, built with
  `encryptedDataStore`). Never `SharedPreferences`, never Room, never logged.
- Logout: call the logout endpoint (best effort), `tokenStorage.clear()`, clear Ktor's cached tokens
  (`authProvider<BearerAuthProvider>()?.clearToken()`), then run every injected `UserDataCleaner`.
- Session expiry reaches the UI through normal layers: `:feature:auth:data:api` exposes
  `SessionRepository.observeSessionExpired()`, `:feature:auth:domain` wraps it in a use case, and
  `:feature:auth:ui` exposes a public `AuthSessionHost(onSessionExpired, content)` built with the
  usual ViewModel/`NavigationHandler` pattern. `:app` wraps `NavDisplay` in it and, on expiry,
  clears the back stack and opens the login key.
