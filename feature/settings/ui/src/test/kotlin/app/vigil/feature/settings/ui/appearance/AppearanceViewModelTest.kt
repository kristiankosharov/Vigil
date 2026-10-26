package app.vigil.feature.settings.ui.appearance

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.feature.settings.domain.model.ThemeMode
import app.vigil.feature.settings.domain.usecase.SetThemeModeUseCase
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class AppearanceViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    // With a StandardTestDispatcher nothing launched in init runs until the scheduler is advanced,
    // so always advance before asserting and read the latest item.

    test("nothing is selected until the stored choice is read") {
        val viewModel = AppearanceViewModel(
            observeThemeMode = { flowOf(AppResult.Success(ThemeMode.Light)) },
            setThemeMode = { AppResult.Success(Unit) },
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().selected shouldBe null

            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().selected shouldBe ThemeMode.Light
        }
    }

    test("Select stores the mode, and the selection follows the store") {
        val stored = MutableStateFlow<AppResult<ThemeMode>>(AppResult.Success(ThemeMode.System))
        val saved = mutableListOf<ThemeMode>()
        val viewModel = AppearanceViewModel(
            observeThemeMode = { stored },
            setThemeMode = { mode ->
                saved += mode
                stored.value = AppResult.Success(mode)
                AppResult.Success(Unit)
            },
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()

            viewModel.onAction(AppearanceAction.Select(ThemeMode.Dark))
            mainDispatcher.scheduler.advanceUntilIdle()
            saved shouldBe listOf(ThemeMode.Dark)
            expectMostRecentItem().selected shouldBe ThemeMode.Dark
        }
    }

    test("selecting the mode that is already chosen stores nothing") {
        var calls = 0
        val viewModel = AppearanceViewModel(
            observeThemeMode = { flowOf(AppResult.Success(ThemeMode.Dark)) },
            setThemeMode = {
                calls++
                AppResult.Success(Unit)
            },
        )
        mainDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(AppearanceAction.Select(ThemeMode.Dark))
        mainDispatcher.scheduler.advanceUntilIdle()
        calls shouldBe 0
    }

    test("a failed save shows a snackbar and keeps the old selection") {
        val viewModel = AppearanceViewModel(
            observeThemeMode = { flowOf(AppResult.Success(ThemeMode.System)) },
            setThemeMode = SetThemeModeUseCase { AppResult.Failure(AppError.Local.Storage(null)) },
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()

            viewModel.onAction(AppearanceAction.Select(ThemeMode.Light))
            mainDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            state.selected shouldBe ThemeMode.System
            state.effect.shouldBeInstanceOf<UiEffect.ShowSnackbar<AppearanceAction>>().action shouldBe null
        }
    }

    test("a failed read shows a retry snackbar; EffectConsumed clears it") {
        val viewModel = AppearanceViewModel(
            observeThemeMode = { flowOf(AppResult.Failure(AppError.Local.Storage(null))) },
            setThemeMode = { AppResult.Success(Unit) },
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            val snackbar = expectMostRecentItem().effect.shouldBeInstanceOf<UiEffect.ShowSnackbar<AppearanceAction>>()
            snackbar.action shouldBe AppearanceAction.Refresh

            viewModel.onAction(AppearanceAction.EffectConsumed(snackbar))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().effect shouldBe null
        }
    }

    test("Refresh subscribes again after the flow ended, and is ignored while it runs") {
        var subscriptions = 0
        val viewModel = AppearanceViewModel(
            observeThemeMode = {
                flow {
                    subscriptions++
                    emit(AppResult.Success(ThemeMode.System))
                }
            },
            setThemeMode = { AppResult.Success(Unit) },
        )
        mainDispatcher.scheduler.advanceUntilIdle()
        subscriptions shouldBe 1

        viewModel.onAction(AppearanceAction.Refresh)
        mainDispatcher.scheduler.advanceUntilIdle()
        subscriptions shouldBe 2
    }
})
