package app.vigil.feature.auth.data.repository

import android.app.Activity
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.feature.auth.data.remote.CurrentActivityTracker
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk

class AuthRepositoryImplTest : FunSpec({
    val auth = mockk<FirebaseAuth>()
    val repository = AuthRepositoryImpl(auth, CurrentActivityTracker())

    test("signing in with email succeeds when Firebase does") {
        every { auth.signInWithEmailAndPassword("ana@example.com", "secret1") } returns
            Tasks.forResult(mockk<AuthResult>())

        repository.signInWithEmail("ana@example.com", "secret1") shouldBe AppResult.Success(Unit)
    }

    test("a Firebase failure becomes an AppError") {
        every { auth.createUserWithEmailAndPassword(any(), any()) } returns
            Tasks.forException(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "bad email"))

        repository.signUpWithEmail("ana@", "secret1")
            .shouldBeInstanceOf<AppResult.Failure>().error.shouldBeInstanceOf<AppError.Auth.InvalidCredentials>()
    }

    test("OIDC without a resumed activity fails instead of crashing") {
        repository.signInWithOidc("oidc.company")
            .shouldBeInstanceOf<AppResult.Failure>().error.shouldBeInstanceOf<AppError.Auth.Unknown>()
    }

    test("the tracker forgets an activity once it pauses") {
        val tracker = CurrentActivityTracker()
        val activity = mockk<Activity>()
        tracker.onActivityResumed(activity)
        tracker.current shouldBe activity
        tracker.onActivityPaused(activity)
        tracker.current shouldBe null
    }

    test("no OIDC provider is configured yet") {
        repository.oidcProviders() shouldBe emptyList()
    }
})
