package app.vigil.common.ui.mvi

import app.vigil.common.ui.text.UiText
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

private object TestAction : UiAction

class UiEffectQueueTest : FunSpec({

    fun toast(text: String) = UiEffect.ShowToast(UiText.Dynamic(text))

    suspend fun UiEffectQueue<TestAction>.drain(): List<UiEffect<TestAction>> = buildList {
        while (true) {
            val head = current ?: break
            add(head)
            consume(head)
        }
    }

    test("concurrent sends from many coroutines lose nothing") {
        val queue = UiEffectQueue<TestAction>()
        val sent = List(500) { toast("effect $it") }

        withContext(Dispatchers.Default) {
            sent.map { effect -> async { queue.send(effect) } }.awaitAll()
        }

        queue.drain() shouldContainExactlyInAnyOrder sent
    }

    test("consume drains in FIFO order") {
        val queue = UiEffectQueue<TestAction>()
        val first = toast("first")
        val second = toast("second")
        val third = toast("third")

        listOf(first, second, third).forEach { queue.send(it) }

        queue.drain() shouldBe listOf(first, second, third)
        queue.current shouldBe null
    }

    test("consuming an effect that is not the head is a no-op") {
        val queue = UiEffectQueue<TestAction>()
        val first = toast("first")
        val second = toast("second")
        queue.send(first)
        queue.send(second)

        queue.consume(second)

        queue.current shouldBeSameInstanceAs first
    }

    test("the same message sent twice is two effects") {
        val queue = UiEffectQueue<TestAction>()
        queue.send(toast("saved"))
        queue.send(toast("saved"))

        queue.drain().size shouldBe 2
    }
})
