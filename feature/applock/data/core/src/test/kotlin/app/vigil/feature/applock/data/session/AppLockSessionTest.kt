package app.vigil.feature.applock.data.session

import app.vigil.feature.applock.data.FakeVault
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

private val KEY = ByteArray(32) { 7 }

class AppLockSessionTest : FunSpec({

    test("a new session starts locked; unlock unlocks it and opens the vault") {
        val vault = FakeVault()
        val session = AppLockSession(TestTimeSource(), vault)
        session.isLocked.value shouldBe true

        session.unlock(KEY)
        session.isLocked.value shouldBe false
        vault.isOpen.value shouldBe true

        session.lock()
        vault.isOpen.value shouldBe false
    }

    test("a short trip to the background keeps it unlocked") {
        val time = TestTimeSource()
        val vault = FakeVault()
        val session = AppLockSession(time, vault).apply { unlock(KEY) }

        session.onBackgrounded()
        time += 29.seconds
        session.onForegrounded()

        session.isLocked.value shouldBe false
    }

    test("the grace period or longer in the background locks it again") {
        val time = TestTimeSource()
        val vault = FakeVault()
        val session = AppLockSession(time, vault).apply { unlock(KEY) }

        session.onBackgrounded()
        time += AppLockSession.GRACE_PERIOD
        session.onForegrounded()

        session.isLocked.value shouldBe true
        vault.isOpen.value shouldBe false
    }

    test("coming to the foreground without a background mark changes nothing") {
        val session = AppLockSession(TestTimeSource(), FakeVault()).apply { unlock(KEY) }

        session.onForegrounded()

        session.isLocked.value shouldBe false
    }
})
