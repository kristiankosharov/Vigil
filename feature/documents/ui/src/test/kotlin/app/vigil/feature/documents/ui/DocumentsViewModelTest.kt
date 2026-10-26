package app.vigil.feature.documents.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import app.vigil.common.core.AppError
import app.vigil.common.core.AppResult
import app.vigil.common.testing.MainDispatcherExtension
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.feature.documents.domain.model.Document
import app.vigil.feature.documents.domain.model.DocumentFilter
import app.vigil.feature.documents.domain.model.DocumentStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class DocumentsViewModelTest : FunSpec({
    val mainDispatcher = MainDispatcherExtension()
    extension(mainDispatcher)

    val rental = Document("1", "Rental agreement", DocumentStatus.ToSign, 4, 3L, "landlord lease")
    val contract = Document("2", "Employment contract", DocumentStatus.Signed, 6, 2L, "employer salary")
    val passport = Document("3", "Passport copy", DocumentStatus.Draft, 1, 1L, "identity")
    val all = listOf(rental, contract, passport)

    fun viewModel(
        documents: () -> Flow<AppResult<List<Document>>> = { flowOf(AppResult.Success(all)) },
        onLock: () -> Unit = {},
    ) = DocumentsViewModel(
        savedStateHandle = SavedStateHandle(),
        observeDocuments = { documents() },
        lockWallet = { onLock() },
    )

    fun DocumentsUiState.ids() = documents.map(DocumentItem::id)

    test("starts loading, then lists every document") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            awaitItem().isLoading shouldBe true
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.isLoading shouldBe false
                it.hasDocuments shouldBe true
                it.ids() shouldBe listOf("1", "2", "3")
            }
        }
    }

    test("a filter keeps only its status") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.onAction(DocumentsAction.FilterSelected(DocumentFilter.Signed))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.filter shouldBe DocumentFilter.Signed
                it.ids() shouldBe listOf("2")
            }
        }
    }

    test("the query applies only while searching; closing search clears it") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.onAction(DocumentsAction.SearchOpened)
            viewModel.onAction(DocumentsAction.QueryChanged("lease"))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.isSearching shouldBe true
                it.ids() shouldBe listOf("1")
            }

            viewModel.onAction(DocumentsAction.SearchClosed)
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.isSearching shouldBe false
                it.query shouldBe ""
                it.ids() shouldBe listOf("1", "2", "3")
            }
        }
    }

    test("filter and search combine") {
        val viewModel = viewModel()

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            viewModel.onAction(DocumentsAction.FilterSelected(DocumentFilter.Drafts))
            viewModel.onAction(DocumentsAction.SearchOpened)
            viewModel.onAction(DocumentsAction.QueryChanged("lease"))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().let {
                it.ids() shouldBe emptyList()
                it.hasDocuments shouldBe true
            }
        }
    }

    test("Lock now locks the wallet") {
        var locks = 0
        val viewModel = viewModel(onLock = { locks++ })

        viewModel.onAction(DocumentsAction.LockNow)

        locks shouldBe 1
    }

    test("a failure shows a retry snackbar; EffectConsumed clears it") {
        val viewModel = viewModel(documents = { flowOf(AppResult.Failure(AppError.Local.Database(null))) })

        moleculeFlow(RecompositionMode.Immediate) { viewModel.uiState() }.test {
            mainDispatcher.scheduler.advanceUntilIdle()
            val snackbar = expectMostRecentItem().effect.shouldBeInstanceOf<UiEffect.ShowSnackbar<DocumentsAction>>()
            snackbar.action shouldBe DocumentsAction.Refresh

            viewModel.onAction(DocumentsAction.EffectConsumed(snackbar))
            mainDispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem().effect shouldBe null
        }
    }

    test("Refresh subscribes again after the flow ended, and is ignored while it runs") {
        var subscriptions = 0
        val viewModel = viewModel(
            documents = {
                flow {
                    subscriptions++
                    emit(AppResult.Success(emptyList()))
                }
            },
        )
        mainDispatcher.scheduler.advanceUntilIdle()
        subscriptions shouldBe 1

        viewModel.onAction(DocumentsAction.Refresh)
        mainDispatcher.scheduler.advanceUntilIdle()
        subscriptions shouldBe 2
    }
})
