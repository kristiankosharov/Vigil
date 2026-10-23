package app.vigil.common.core

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.IOException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class RetryingResultTest : FunSpec({

    fun Flow<Int>.retrying(maxRetries: Int = 3) =
        asRetryingResult(mapError = { AppError.Local.Database(it) }, maxRetries = maxRetries)

    test("emits each value as Success") {
        runTest {
            flowOf(1, 2).retrying().test {
                awaitItem() shouldBe AppResult.Success(1)
                awaitItem() shouldBe AppResult.Success(2)
                awaitComplete()
            }
        }
    }

    test("a failure is emitted, then the source is re-subscribed and recovers") {
        runTest {
            scripted(false, true).retrying().test {
                awaitItem().shouldBeFailure()
                awaitItem() shouldBe AppResult.Success(2)
                expectNoEvents()
                cancel()
            }
        }
    }

    test("a success resets the counter: fail x3, succeed, fail x3 keeps the flow alive") {
        runTest {
            scripted(false, false, false, true, false, false, false).retrying(maxRetries = 3).test {
                repeat(3) { awaitItem().shouldBeFailure() }
                awaitItem() shouldBe AppResult.Success(4)
                repeat(3) { awaitItem().shouldBeFailure() }
                expectNoEvents() // still subscribed, not completed
                cancel()
            }
        }
    }

    test("maxRetries + 1 consecutive failures end the flow, keeping the last failure") {
        runTest {
            scripted(false, false, false, false, true).retrying(maxRetries = 3).test {
                repeat(4) { awaitItem().shouldBeFailure() }
                awaitComplete()
            }
        }
    }

    test("two collectors keep separate counters") {
        runTest {
            val alwaysFails = flow<Int> { throw IOException("down") }.retrying(maxRetries = 1)
            val counts = IntArray(2)
            val jobs = List(2) { index -> launch { alwaysFails.collect { counts[index]++ } } }
            jobs.forEach { it.join() }
            counts.toList() shouldBe listOf(2, 2)
        }
    }
})

/**
 * A source that, across re-subscriptions, emits its 1-based position for each `true` and throws an
 * IOException for each `false`, then stays subscribed.
 */
private fun scripted(vararg outcomes: Boolean): Flow<Int> {
    var position = 0
    return flow {
        while (position < outcomes.size) {
            val succeeds = outcomes[position++]
            if (succeeds) emit(position) else throw IOException("failure $position")
        }
        awaitCancellation()
    }
}

private fun AppResult<Int>.shouldBeFailure() = shouldBeInstanceOf<AppResult.Failure>()
