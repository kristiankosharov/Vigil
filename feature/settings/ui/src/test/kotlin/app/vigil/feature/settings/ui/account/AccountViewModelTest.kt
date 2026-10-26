package app.vigil.feature.settings.ui.account

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.feature.settings.domain.model.ThemeMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf

class AccountViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    test("shows the stored theme once it is read") {
        val viewModel = AccountViewModel(observeThemeMode = { flowOf(AppResult.Success(ThemeMode.Dark)) })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().themeMode shouldBe null
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().themeMode shouldBe ThemeMode.Dark
        }
    }

    test("a failed read leaves the current value empty") {
        val viewModel = AccountViewModel(
            observeThemeMode = { flowOf(AppResult.Failure(AppError.Local.Storage(null))) },
        )

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().themeMode shouldBe null
        }
    }
})
