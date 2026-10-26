package app.vigil.feature.applock.data.session

import app.vigil.feature.applock.data.FakeVault
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private val KEY = ByteArray(32) { 7 }

class AppLockSessionTest : FunSpec({

    test("a new session starts locked; unlock unlocks it and opens the vault") {
        runTest {
            val vault = FakeVault()
            val session = AppLockSession(testScheduler.timeSource, vault, backgroundScope)
            session.isLocked.value shouldBe true

            session.unlock(KEY)
            session.isLocked.value shouldBe false
            vault.isOpen.value shouldBe true

            session.lock()
            vault.isOpen.value shouldBe false
        }
    }

    test("a short trip to the background keeps it unlocked") {
        runTest {
            val vault = FakeVault()
            val session = unlockedSession(vault)

            session.onBackgrounded()
            advanceTimeBy(4.seconds)
            session.onForegrounded()
            advanceTimeBy(AppLockSession.GRACE_PERIOD) // the cancelled timer never fires
            runCurrent()

            session.isLocked.value shouldBe false
            vault.isOpen.value shouldBe true
        }
    }

    test("it locks after the grace period while still in the background") {
        runTest {
            val vault = FakeVault()
            val session = unlockedSession(vault)

            session.onBackgrounded()
            advanceTimeBy(AppLockSession.GRACE_PERIOD)
            runCurrent()

            session.isLocked.value shouldBe true
            vault.isOpen.value shouldBe false
        }
    }

    test("a frozen process (the timer didn't run) locks when it comes back after the grace period") {
        runTest {
            val time = TestTimeSource()
            val vault = FakeVault()
            val session = AppLockSession(time, vault, backgroundScope).apply { unlock(KEY) }

            session.onBackgrounded()
            time += AppLockSession.GRACE_PERIOD
            session.onForegrounded()

            session.isLocked.value shouldBe true
            vault.isOpen.value shouldBe false
        }
    }

    test("coming to the foreground without a background mark changes nothing") {
        runTest {
            val session = unlockedSession(FakeVault())

            session.onForegrounded()

            session.isLocked.value shouldBe false
        }
    }
})

private fun TestScope.unlockedSession(vault: FakeVault) =
    AppLockSession(testScheduler.timeSource, vault, backgroundScope).apply { unlock(KEY) }
