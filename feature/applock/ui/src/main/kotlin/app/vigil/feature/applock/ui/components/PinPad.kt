package app.vigil.feature.applock.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.theme.SecurityTheme
import app.vigil.common.ui.theme.statusColors
import app.vigil.feature.applock.domain.model.PinRules
import app.vigil.feature.applock.ui.R

private val KeySize = 76.dp
private val DotSize = 14.dp
private val KeyGapHorizontal = 28.dp
private val KeyGapVertical = 14.dp
private const val SHAKE_MILLIS = 400
private const val SHAKE_STEP_MILLIS = 50

/** Horizontal offsets (dp) of one shake: out and back, fading. */
@Suppress("MagicNumber") // the offsets are the animation itself
private val ShakeOffsets = floatArrayOf(-12f, 12f, -8f, 8f, -4f, 4f, 0f)

/** 1 2 3 / 4 5 6 / 7 8 9, like a phone keypad; 0 sits below. */
private val DigitRows = "123456789".map(Char::digitToInt).chunked(KEYS_PER_ROW)
private const val KEYS_PER_ROW = 3

/** How the PIN dots read: still typing, wrong (shakes once) or confirmed. */
internal enum class PinDotsStatus { Typing, Error, Success }

/**
 * One dot per digit of the PIN: filled for entered digits, outlined for the rest. The PIN itself is
 * never shown or read out; screen readers hear only how many digits were entered.
 */
@Composable
internal fun PinDots(length: Int, status: PinDotsStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        PinDotsStatus.Typing -> MaterialTheme.colorScheme.primary
        PinDotsStatus.Error -> MaterialTheme.colorScheme.error
        PinDotsStatus.Success -> MaterialTheme.statusColors.success
    }
    val emptyColor = MaterialTheme.colorScheme.outline
    val description = pluralStringResource(R.plurals.applock_pin_progress, length, length, PinRules.LENGTH)
    val shake = remember { Animatable(0f) }
    LaunchedEffect(status) {
        if (status == PinDotsStatus.Error) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = SHAKE_MILLIS
                    ShakeOffsets.forEachIndexed { index, offset ->
                        offset at (index + 1) * SHAKE_STEP_MILLIS
                    }
                },
            )
        }
    }
    Row(
        modifier = modifier
            .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
            .clearAndSetSemantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(PinRules.LENGTH) { index ->
            val dot = Modifier
                .size(DotSize)
                .clip(CircleShape)
            Box(if (index < length) dot.background(color) else dot.border(2.dp, emptyColor, CircleShape))
        }
    }
}

/**
 * Digits 1–9, 0 and delete as large round keys; replaces the system keyboard for PIN entry. When
 * [onBiometric] is set, a fingerprint key fills the slot left of 0.
 */
@Composable
internal fun PinPad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    isDeleteEnabled: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onBiometric: (() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KeyGapVertical)) {
        DigitRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGapHorizontal)) {
                row.forEach { digit -> DigitKey(digit = digit, onClick = { onDigit(digit) }, enabled = enabled) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGapHorizontal)) {
            if (onBiometric != null) {
                IconKey(
                    icon = ImageVector.vectorResource(CommonR.drawable.common_ic_fingerprint),
                    description = stringResource(R.string.applock_use_biometric),
                    onClick = onBiometric,
                    enabled = enabled,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Spacer(Modifier.size(KeySize))
            }
            DigitKey(digit = 0, onClick = { onDigit(0) }, enabled = enabled)
            IconKey(
                icon = ImageVector.vectorResource(CommonR.drawable.common_ic_backspace),
                description = stringResource(R.string.applock_delete_digit),
                onClick = onDelete,
                enabled = enabled && isDeleteEnabled,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DigitKey(digit: Int, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Pressed keys switch to primaryContainer (docs/design/README.md, States).
    val color by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "keyColor",
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = color,
        contentColor = MaterialTheme.colorScheme.onSurface,
        interactionSource = interactionSource,
        modifier = modifier.size(KeySize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit.toString(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun IconKey(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier
            .size(KeySize)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
        }
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun PinPadPreview() {
    SecurityTheme {
        Surface {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PinDots(length = 3, status = PinDotsStatus.Typing)
                PinDots(length = 6, status = PinDotsStatus.Success)
                PinPad(onDigit = {}, onDelete = {}, isDeleteEnabled = true, onBiometric = {})
            }
        }
    }
}
