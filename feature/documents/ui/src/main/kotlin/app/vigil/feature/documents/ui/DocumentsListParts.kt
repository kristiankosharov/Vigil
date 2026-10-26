package app.vigil.feature.documents.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.designsystem.AppFilterChip
import app.vigil.feature.documents.domain.model.DocumentFilter

// The parts of the Documents list above and instead of the document cards.

@Composable
internal fun SecurityBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_shield_check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.documents_security_banner),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
internal fun FilterChips(selected: DocumentFilter, onSelect: (DocumentFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DocumentFilter.entries.forEach { filter ->
            AppFilterChip(
                label = stringResource(filter.labelRes()),
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

private fun DocumentFilter.labelRes() = when (this) {
    DocumentFilter.All -> R.string.documents_filter_all
    DocumentFilter.ToSign -> R.string.documents_filter_to_sign
    DocumentFilter.Signed -> R.string.documents_filter_signed
    DocumentFilter.Drafts -> R.string.documents_filter_drafts
}

/** Empty wallet, nothing with this status, or nothing matching the search. */
@Composable
internal fun EmptyState(state: DocumentsUiState, modifier: Modifier = Modifier) {
    val query = state.query.trim()
    val (title, message) = when {
        state.isSearching && query.isNotEmpty() ->
            stringResource(R.string.documents_no_match_title, query) to
                stringResource(R.string.documents_no_match_message)

        state.hasDocuments ->
            stringResource(R.string.documents_empty_filter_title) to
                stringResource(R.string.documents_empty_filter_message)

        else -> stringResource(R.string.documents_empty_title) to stringResource(R.string.documents_empty_message)
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 28.dp)
                .semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
