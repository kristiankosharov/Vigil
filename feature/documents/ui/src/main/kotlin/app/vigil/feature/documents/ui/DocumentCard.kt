package app.vigil.feature.documents.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.theme.statusColors
import app.vigil.feature.documents.domain.model.DocumentStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val CardShape = RoundedCornerShape(16.dp)

/**
 * A document in the list (docs/design/README.md, "Document list item"): thumbnail with the status
 * as a colored corner icon (named for screen readers), title, and "4 pages · 12 Oct 2026".
 */
@Composable
internal fun DocumentCard(item: DocumentItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Thumbnail(item.status)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = item.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = documentMeta(item.pageCount, item.addedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Thumbnail(status: DocumentStatus, modifier: Modifier = Modifier) {
    Box(modifier.padding(end = 4.dp, bottom = 4.dp)) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 56.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(6.dp))
                .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_document_lines),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        StatusBadge(status, Modifier.align(Alignment.BottomEnd).offset(x = 7.dp, y = 7.dp))
    }
}

/** Never color alone: each status has its own icon, and its name is read out. */
@Composable
private fun StatusBadge(status: DocumentStatus, modifier: Modifier = Modifier) {
    val (color, icon, name) = when (status) {
        DocumentStatus.ToSign -> Triple(
            MaterialTheme.statusColors.warning,
            CommonR.drawable.common_ic_pen,
            R.string.documents_status_to_sign,
        )

        DocumentStatus.Signed -> Triple(
            MaterialTheme.statusColors.success,
            CommonR.drawable.common_ic_check,
            R.string.documents_status_signed,
        )

        DocumentStatus.Draft -> Triple(
            MaterialTheme.colorScheme.outline,
            CommonR.drawable.common_ic_more,
            R.string.documents_status_draft,
        )
    }
    val description = stringResource(name)
    val cardColor = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(
        modifier = modifier
            .size(22.dp)
            .background(color, CircleShape)
            .border(2.dp, cardColor, CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        // The card color reads on every status color, in light and dark.
        Icon(
            ImageVector.vectorResource(icon),
            contentDescription = null,
            tint = cardColor,
            modifier = Modifier.size(11.dp),
        )
    }
}

/** "4 pages · Scanned today" or "4 pages · 12 Oct 2026", in the device's locale and time zone. */
@Composable
private fun documentMeta(pageCount: Int, addedAt: Long): String {
    val pages = pluralStringResource(R.plurals.documents_pages, pageCount, pageCount)
    val today = stringResource(R.string.documents_scanned_today)
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(addedAt, locale, today) {
        val zone = ZoneId.systemDefault()
        val day = Instant.ofEpochMilli(addedAt).atZone(zone).toLocalDate()
        if (day == LocalDate.now(zone)) {
            today
        } else {
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            day.format(formatter.withLocale(locale))
        }
    }
    return stringResource(R.string.documents_meta, pages, date)
}
