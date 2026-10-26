package app.vigil.feature.documents.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import app.vigil.common.core.onFailure
import app.vigil.common.core.onSuccess
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.mvi.BaseViewModel
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.text.UiText
import app.vigil.common.ui.text.toUiText
import app.vigil.feature.documents.domain.model.Document
import app.vigil.feature.documents.domain.model.DocumentFilter
import app.vigil.feature.documents.domain.usecase.LockWalletUseCase
import app.vigil.feature.documents.domain.usecase.ObserveDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
internal class DocumentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeDocuments: ObserveDocumentsUseCase,
    private val lockWallet: LockWalletUseCase,
) : BaseViewModel<DocumentsUiState, DocumentsAction, Nothing>() {

    private var filter by savedStateHandle.saveable { mutableStateOf(DocumentFilter.All) }
    private var isSearching by savedStateHandle.saveable { mutableStateOf(false) }
    private var query by savedStateHandle.saveable { mutableStateOf("") }
    private var isLoading by mutableStateOf(true)
    private var documents by mutableStateOf(emptyList<Document>())
    private var observeJob: Job? = null

    init {
        observe()
    }

    @Composable
    override fun uiState(): DocumentsUiState {
        val activeQuery = if (isSearching) query else ""
        val shown = remember(documents, filter, activeQuery) {
            documents
                .filter { filter.accepts(it.status) && it.matches(activeQuery) }
                .map(Document::toItem)
                .toImmutableList()
        }
        return DocumentsUiState(
            isLoading = isLoading,
            filter = filter,
            isSearching = isSearching,
            query = query,
            documents = shown,
            hasDocuments = documents.isNotEmpty(),
            effect = currentEffect,
        )
    }

    override fun onAction(action: DocumentsAction) {
        when (action) {
            is DocumentsAction.FilterSelected -> filter = action.filter

            DocumentsAction.SearchOpened -> isSearching = true

            DocumentsAction.SearchClosed -> {
                isSearching = false
                query = ""
            }

            is DocumentsAction.QueryChanged -> query = action.query

            DocumentsAction.LockNow -> lockWallet()

            DocumentsAction.Refresh -> observe()

            is DocumentsAction.EffectConsumed -> consumeEffect(action.effect)
        }
    }

    /** Subscribes to the stored documents; after the flow gave up (repeated failures), Refresh subscribes again. */
    private fun observe() {
        if (observeJob?.isActive == true) return
        observeJob = observeDocuments()
            .onEach { result ->
                isLoading = false
                result
                    .onSuccess { documents = it }
                    .onFailure { error ->
                        sendEffect(
                            UiEffect.ShowSnackbar(
                                message = error.toUiText(),
                                actionLabel = UiText.Resource(CommonR.string.common_retry),
                                action = DocumentsAction.Refresh,
                            ),
                        )
                    }
            }
            .launchIn(viewModelScope)
    }
}
