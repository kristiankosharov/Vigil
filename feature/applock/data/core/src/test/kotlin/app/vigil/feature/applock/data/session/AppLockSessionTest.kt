package app.vigil.feature.applock.data.session

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class AppLockSessionTest : FunSpec({

    test("a new session starts locked; unlock unlocks it") {
        val session = AppLockSession(TestTimeSource())
        session.isLocked.value shouldBe true

        session.unlock()
        session.isLocked.value shouldBe false
    }

    test("a short trip to the background keeps it unlocked") {
        val time = TestTimeSource()
        val session = AppLockSession(time).apply { unlock() }

        session.onBackgrounded()
        time += 29.seconds
        session.onForegrounded()

        session.isLocked.value shouldBe false
    }

    test("the grace period or longer in the background locks it again") {
        val time = TestTimeSource()
        val session = AppLockSession(time).apply { unlock() }

        session.onBackgrounded()
        time += AppLockSession.GRACE_PERIOD
        session.onForegrounded()

        session.isLocked.value shouldBe true
    }

    test("coming to the foreground without a background mark changes nothing") {
        val session = AppLockSession(TestTimeSource()).apply { unlock() }

        session.onForegrounded()

        session.isLocked.value shouldBe false
    }
})
