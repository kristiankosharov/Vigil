package app.vigil.feature.settings.ui.theme

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.feature.settings.domain.model.ThemeMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class AppThemeViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    test("follows the system until the stored choice is read, then uses it") {
        val viewModel = AppThemeViewModel(observeThemeMode = { flowOf(AppResult.Success(ThemeMode.Dark)) })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().themeMode shouldBe ThemeMode.System

            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().themeMode shouldBe ThemeMode.Dark
        }
    }

    test("a new choice is applied while the app runs") {
        val stored = MutableStateFlow<AppResult<ThemeMode>>(AppResult.Success(ThemeMode.Light))
        val viewModel = AppThemeViewModel(observeThemeMode = { stored })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().themeMode shouldBe ThemeMode.Light

            stored.value = AppResult.Success(ThemeMode.Dark)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().themeMode shouldBe ThemeMode.Dark
        }
    }

    test("a failed read keeps the last theme") {
        val stored = MutableStateFlow<AppResult<ThemeMode>>(AppResult.Success(ThemeMode.Dark))
        val viewModel = AppThemeViewModel(observeThemeMode = { stored })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().themeMode shouldBe ThemeMode.Dark

            stored.value = AppResult.Failure(AppError.Local.Storage(null))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectNoEvents() // no new state: still dark
        }
    }
})
