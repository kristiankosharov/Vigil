package app.vigil.feature.applock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.applock.domain.model.PinRules
import app.vigil.feature.applock.ui.R

private val KeySize = 72.dp
private val DotSize = 14.dp

/** 1 2 3 / 4 5 6 / 7 8 9, like a phone keypad; 0 sits below. */
private val DigitRows = "123456789".map(Char::digitToInt).chunked(KEYS_PER_ROW)
private const val KEYS_PER_ROW = 3

/**
 * One dot per digit: filled for entered digits, outlined for the minimum still missing. The PIN
 * itself is never shown or read out; screen readers hear only how many digits were entered.
 */
@Composable
internal fun PinDots(length: Int, isError: Boolean, modifier: Modifier = Modifier) {
    val slots = maxOf(length, PinRules.MIN_LENGTH)
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val description = pluralStringResource(R.plurals.applock_pin_progress, length, length)
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(slots) { index ->
            val dot = Modifier
                .size(DotSize)
                .clip(CircleShape)
            Box(if (index < length) dot.background(color) else dot.border(2.dp, color, CircleShape))
        }
    }
}

/** Digits 1–9, 0 and delete, as large round keys. Replaces the system keyboard for PIN entry. */
@Composable
internal fun PinPad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    isDeleteEnabled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DigitRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { digit -> DigitKey(digit = digit, onClick = { onDigit(digit) }, enabled = enabled) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spacer(Modifier.size(KeySize))
            DigitKey(digit = 0, onClick = { onDigit(0) }, enabled = enabled)
            DeleteKey(onClick = onDelete, enabled = enabled && isDeleteEnabled)
        }
    }
}

@Composable
private fun DigitKey(digit: Int, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    PadKey(onClick = onClick, enabled = enabled, modifier = modifier) {
        Text(text = digit.toString(), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun DeleteKey(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.applock_delete_digit)
    PadKey(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        isSecondary = true,
    ) {
        Text(text = "⌫", style = MaterialTheme.typography.headlineSmall) // a symbol, not language text
    }
}

@Composable
private fun PadKey(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (isSecondary) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.size(KeySize),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun PinPadPreview() {
    AppTheme(dynamicColor = false) {
        Surface {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PinDots(length = 3, isError = false)
                PinPad(onDigit = {}, onDelete = {}, isDeleteEnabled = true)
            }
        }
    }
}
