package app.vigil.feature.documents.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.vigil.common.ui.R as CommonR

/** "Documents" with search and lock-now buttons. */
@Composable
internal fun DocumentsTitleBar(onSearch: () -> Unit, onLockNow: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.documents_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.02).em,
            ),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(onClick = onSearch) {
            Icon(
                imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_search),
                contentDescription = stringResource(R.string.documents_search),
            )
        }
        IconButton(onClick = onLockNow) {
            Icon(
                imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_lock),
                contentDescription = stringResource(R.string.documents_lock_now),
            )
        }
    }
}

/** Search replaces the title: back closes it, the pill holds the query and a clear button. */
@Composable
internal fun DocumentsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() } // the keyboard opens with search
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_back),
                contentDescription = stringResource(R.string.documents_close_search),
            )
        }
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
        )
    }
}

/** The rounded field: search icon, the query (or a hint), and a clear button once there's text. */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val hint = stringResource(R.string.documents_search_hint)
    val label = stringResource(R.string.documents_search)
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .semantics { contentDescription = label },
            decorationBox = { field ->
                if (query.isEmpty()) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                field()
            },
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_close),
                    contentDescription = stringResource(R.string.documents_clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
