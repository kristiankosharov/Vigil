package app.vigil.feature.auth.data.repository

import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.network.AuthTokens
import app.vigil.common.network.TokenStorage
import app.vigil.feature.auth.data.remote.AuthApi
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk

private val BACKEND_TOKENS = AuthTokens(accessToken = "access", refreshToken = "refresh")

class AuthRepositoryImplTest : FunSpec({

    test("signing in with email exchanges Firebase's ID token, saves the backend tokens") {
        val backend = Backend()
        every { backend.auth.signInWithEmailAndPassword("ana@example.com", "secret1") } returns
            Tasks.forResult(mockk<AuthResult>())

        backend.repository.signInWithEmail("ana@example.com", "secret1") shouldBe AppResult.Success(Unit)

        backend.exchangedIdTokens shouldBe listOf("firebase-id-token")
        backend.tokens.saved shouldBe BACKEND_TOKENS
    }

    test("a failed exchange fails the sign-in, saves nothing") {
        val backend = Backend(exchange = AppResult.Failure(AppError.Network.NoConnection(null)))
        every { backend.auth.signInWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockk<AuthResult>())

        backend.repository.signInWithEmail("ana@example.com", "secret1")
            .shouldBeInstanceOf<AppResult.Failure>().error.shouldBeInstanceOf<AppError.Network.NoConnection>()

        backend.tokens.saved.shouldBeNull()
    }

    test("signed in without a Firebase user: an error, no exchange") {
        val backend = Backend()
        every { backend.auth.currentUser } returns null
        every { backend.auth.createUserWithEmailAndPassword(any(), any()) } returns Tasks.forResult(mockk<AuthResult>())

        backend.repository.signUpWithEmail("ana@example.com", "secret1")
            .shouldBeInstanceOf<AppResult.Failure>().error.shouldBeInstanceOf<AppError.Auth.Unknown>()

        backend.exchangedIdTokens shouldBe emptyList()
        backend.tokens.saved.shouldBeNull()
    }

    test("a Firebase failure becomes an AppError, with no exchange") {
        val backend = Backend()
        every { backend.auth.createUserWithEmailAndPassword(any(), any()) } returns
            Tasks.forException(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "bad email"))

        backend.repository.signUpWithEmail("ana@", "secret1")
            .shouldBeInstanceOf<AppResult.Failure>().error.shouldBeInstanceOf<AppError.Auth.InvalidCredentials>()

        backend.exchangedIdTokens shouldBe emptyList()
    }

    test("no OIDC provider is configured yet") {
        Backend().repository.oidcProviders() shouldBe emptyList()
    }
})

/** Firebase (mocked, signed in as a user with an ID token), the backend API and the token storage. */
private class Backend(exchange: AppResult<AuthTokens> = AppResult.Success(BACKEND_TOKENS)) {
    val exchangedIdTokens = mutableListOf<String>()
    val tokens = RecordingTokenStorage()
    val auth = mockk<FirebaseAuth>(relaxUnitFun = true) {
        val idToken = mockk<GetTokenResult> { every { token } returns "firebase-id-token" }
        every { currentUser } returns
            mockk<FirebaseUser> { every { getIdToken(false) } returns Tasks.forResult(idToken) }
    }
    val api = object : AuthApi {
        override suspend fun exchangeFirebaseToken(idToken: String): AppResult<AuthTokens> {
            exchangedIdTokens += idToken
            return exchange
        }
    }
    val repository = AuthRepositoryImpl(auth, BackendSession(auth, api, tokens))
}

private class RecordingTokenStorage : TokenStorage {
    var saved: AuthTokens? = null

    override suspend fun read() = saved

    override suspend fun save(tokens: AuthTokens) {
        saved = tokens
    }

    override suspend fun clear() {
        saved = null
    }
}
