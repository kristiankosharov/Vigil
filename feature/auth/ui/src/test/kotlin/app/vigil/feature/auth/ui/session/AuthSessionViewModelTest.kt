package app.vigil.feature.auth.ui.session

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.testing.MainDispatcherExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow

class AuthSessionViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    test("session expiry becomes SessionExpired navigation, consumed by NavigationConsumed") {
        val expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val viewModel = AuthSessionViewModel(observeSessionExpired = { expired })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().navigation shouldBe null

            expired.tryEmit(Unit)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().navigation shouldBe AuthSessionNavigation.SessionExpired

            viewModel.onAction(AuthSessionAction.NavigationConsumed(AuthSessionNavigation.SessionExpired))
            awaitItem().navigation shouldBe null
        }
    }
})
