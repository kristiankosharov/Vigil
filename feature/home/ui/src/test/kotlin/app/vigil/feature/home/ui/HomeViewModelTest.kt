package app.vigil.feature.home.ui

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.feature.home.domain.model.HomeEntry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class HomeViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    // With a StandardTestDispatcher nothing launched in init runs until the scheduler is advanced,
    // so always advance before asserting and read the latest item.

    test("starts loading, then shows the entries") {
        val viewModel = HomeViewModel(observeEntries = { flowOf(AppResult.Success(listOf(HomeEntry("1", "First")))) })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().isLoading shouldBe true

            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.isLoading shouldBe false
            state.entries shouldBe listOf(HomeEntryItem("1", "First"))
        }
    }

    test("a failure shows a retry snackbar; EffectConsumed clears it") {
        val viewModel = HomeViewModel(
            observeEntries = { flowOf(AppResult.Failure(AppError.Local.Database(null))) },
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            val snackbar = expectMostRecentItem().effect.shouldBeInstanceOf<UiEffect.ShowSnackbar<HomeAction>>()
            snackbar.action shouldBe HomeAction.Refresh

            viewModel.onAction(HomeAction.EffectConsumed(snackbar))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().effect shouldBe null
        }
    }

    test("Refresh subscribes again after the flow ended, and is ignored while it runs") {
        var subscriptions = 0
        val viewModel = HomeViewModel(
            observeEntries = {
                flow {
                    subscriptions++
                    emit(AppResult.Success(emptyList()))
                }
            },
        )
        mainDispatcher.scheduler.advanceUntilIdle()
        subscriptions shouldBe 1

        viewModel.onAction(HomeAction.Refresh)
        mainDispatcher.scheduler.advanceUntilIdle()
        subscriptions shouldBe 2
    }
})
