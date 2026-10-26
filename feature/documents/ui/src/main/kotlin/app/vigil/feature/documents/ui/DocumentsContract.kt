package app.vigil.feature.documents.ui

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiEffect
import app.vigil.common.ui.mvi.UiState
import app.vigil.feature.documents.domain.model.Document
import app.vigil.feature.documents.domain.model.DocumentFilter
import app.vigil.feature.documents.domain.model.DocumentStatus
import kotlinx.collections.immutable.ImmutableList

/** The Documents tab (docs/design/README.md, 2.1). */
@Immutable
internal data class DocumentsUiState(
    val isLoading: Boolean,
    val filter: DocumentFilter,
    val isSearching: Boolean,
    val query: String,
    /** The documents that pass [filter] and [query]. */
    val documents: ImmutableList<DocumentItem>,
    /** Whether the wallet has any document at all, to tell "empty wallet" from "nothing matches". */
    val hasDocuments: Boolean,
    val effect: UiEffect<DocumentsAction>?,
) : UiState

@Immutable
internal data class DocumentItem(
    val id: String,
    val title: String,
    val status: DocumentStatus,
    val pageCount: Int,
    /** Epoch milliseconds. */
    val addedAt: Long,
)

internal fun Document.toItem() =
    DocumentItem(id = id, title = title, status = status, pageCount = pageCount, addedAt = addedAt)

internal sealed interface DocumentsAction : UiAction {
    data class FilterSelected(val filter: DocumentFilter) : DocumentsAction

    data object SearchOpened : DocumentsAction

    /** Closes search and clears the query. */
    data object SearchClosed : DocumentsAction

    data class QueryChanged(val query: String) : DocumentsAction

    data object LockNow : DocumentsAction

    data object Refresh : DocumentsAction

    class EffectConsumed(val effect: UiEffect<DocumentsAction>) : DocumentsAction
}
