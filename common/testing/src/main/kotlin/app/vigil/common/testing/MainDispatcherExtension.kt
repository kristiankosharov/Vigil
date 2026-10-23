package app.vigil.common.testing

import io.kotest.core.listeners.AfterTestListener
import io.kotest.core.listeners.BeforeTestListener
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Replaces `Dispatchers.Main` with a [StandardTestDispatcher] around every test of the spec:
 * `extension(MainDispatcherExtension())`. Nothing launched on Main (e.g. in a ViewModel's `init`)
 * runs until [scheduler] is advanced.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(val dispatcher: TestDispatcher = StandardTestDispatcher()) :
    BeforeTestListener,
    AfterTestListener {

    val scheduler: TestCoroutineScheduler get() = dispatcher.scheduler

    override suspend fun beforeTest(testCase: TestCase) {
        Dispatchers.setMain(dispatcher)
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        Dispatchers.resetMain()
    }
}
