package app.vigil.feature.documents.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.designsystem.AppFilterChip
import app.vigil.common.ui.mvi.UiEffectHandler
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.documents.domain.model.DocumentFilter
import app.vigil.feature.documents.domain.model.DocumentStatus
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** The Documents tab. [onOpenDocument] gets the id of the tapped document. */
@Composable
fun DocumentsRoute(onOpenDocument: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: DocumentsViewModel = hiltViewModel()
    DocumentsScreen(
        state = viewModel.uiState(),
        onAction = viewModel::onAction,
        onOpenDocument = onOpenDocument,
        modifier = modifier,
    )
}

@Composable
internal fun DocumentsScreen(
    state: DocumentsUiState,
    onAction: (DocumentsAction) -> Unit,
    onOpenDocument: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { onAction(DocumentsAction.EffectConsumed(it)) },
    )
    BackHandler(enabled = state.isSearching) { onAction(DocumentsAction.SearchClosed) }

    Scaffold(
        modifier = modifier,
        // The tab bar below already handles the bottom inset.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            if (state.isSearching) {
                DocumentsSearchBar(
                    query = state.query,
                    onQueryChange = { onAction(DocumentsAction.QueryChanged(it)) },
                    onClose = { onAction(DocumentsAction.SearchClosed) },
                )
            } else {
                DocumentsTitleBar(
                    onSearch = { onAction(DocumentsAction.SearchOpened) },
                    onLockNow = { onAction(DocumentsAction.LockNow) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            val description = stringResource(R.string.documents_loading)
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.semantics { contentDescription = description })
            }
        } else {
            DocumentList(state = state, onAction = onAction, onOpenDocument = onOpenDocument, contentPadding = padding)
        }
    }
}

@Composable
private fun DocumentList(
    state: DocumentsUiState,
    onAction: (DocumentsAction) -> Unit,
    onOpenDocument: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!state.isSearching) {
            item(key = "banner") { SecurityBanner(Modifier.padding(horizontal = 20.dp)) }
        }
        item(key = "filters") {
            FilterChips(
                selected = state.filter,
                onSelect = { onAction(DocumentsAction.FilterSelected(it)) },
                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
            )
        }
        items(state.documents, key = DocumentItem::id) { item ->
            DocumentCard(item = item, onClick = {
                onOpenDocument(item.id)
            }, modifier = Modifier.padding(horizontal = 20.dp))
        }
        if (state.documents.isEmpty()) {
            item(key = "empty") { EmptyState(state, Modifier.padding(horizontal = 20.dp)) }
        }
    }
}

private const val DAY_MILLIS = 86_400_000L
private const val PREVIEW_NOW = 1_793_000_000_000L

private val PreviewDocuments = persistentListOf(
    DocumentItem("1", "Rental agreement", DocumentStatus.ToSign, pageCount = 4, addedAt = PREVIEW_NOW),
    DocumentItem("2", "Employment contract", DocumentStatus.Signed, pageCount = 6, addedAt = PREVIEW_NOW - DAY_MILLIS),
    DocumentItem("3", "Passport copy", DocumentStatus.Draft, pageCount = 1, addedAt = PREVIEW_NOW - 2 * DAY_MILLIS),
)

private fun previewState(
    documents: List<DocumentItem> = PreviewDocuments,
    isSearching: Boolean = false,
    query: String = "",
) = DocumentsUiState(
    isLoading = false,
    filter = DocumentFilter.All,
    isSearching = isSearching,
    query = query,
    documents = documents.toImmutableList(),
    hasDocuments = documents.isNotEmpty(),
    effect = null,
)

@PreviewLightDark
@PreviewFontScale
@Composable
private fun DocumentsPreview() {
    AppTheme { DocumentsScreen(state = previewState(), onAction = {}, onOpenDocument = {}) }
}

@PreviewLightDark
@Composable
private fun DocumentsEmptyPreview() {
    AppTheme { DocumentsScreen(state = previewState(documents = emptyList()), onAction = {}, onOpenDocument = {}) }
}

@PreviewLightDark
@Composable
private fun DocumentsNoMatchPreview() {
    AppTheme {
        DocumentsScreen(
            state = previewState(documents = emptyList(), isSearching = true, query = "insurance"),
            onAction = {},
            onOpenDocument = {},
        )
    }
}
