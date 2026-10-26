package app.vigil.common.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.vigil.common.ui.theme.AppTheme

// States follow Material 3: pressed = 10% state layer, disabled = onSurface 12% container / 38% content.
// Every button takes an optional interactionSource so previews and tests can drive the pressed state.

private val ButtonShape = RoundedCornerShape(16.dp)

/** Main call to action of a screen, full width, 56 dp. [isError] is for destructive or retry actions. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    icon: ImageVector? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        enabled = enabled,
        shape = ButtonShape,
        colors = if (isError) {
            ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError)
        } else {
            ButtonDefaults.buttonColors()
        },
        contentPadding = PaddingValues(horizontal = 24.dp),
        interactionSource = interactionSource,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Secondary action with an outline, e.g. "Retake". [isError] colors border and text with `error`. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val borderColor = when {
        !enabled -> scheme.onSurface.copy(alpha = 0.12f)
        isError -> scheme.error
        else -> scheme.outline
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = ButtonShape,
        colors = if (isError) {
            ButtonDefaults.outlinedButtonColors(contentColor = scheme.error)
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Low-emphasis action, e.g. "Save without signing". [isError] for destructive ones like "Remove reminder". */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = ButtonShape,
        colors = if (isError) {
            ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.textButtonColors()
        },
        interactionSource = interactionSource,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun ButtonsPreview() {
    AppTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(text = "Approve & sign", onClick = {})
                PrimaryButton(text = "Approve & sign", onClick = {}, enabled = false)
                PrimaryButton(text = "Try again", onClick = {}, isError = true)
                SecondaryButton(text = "Retake", onClick = {})
                SecondaryButton(text = "Delete", onClick = {}, isError = true)
                TertiaryButton(text = "Save without signing", onClick = {})
                TertiaryButton(text = "Remove reminder", onClick = {}, isError = true)
            }
        }
    }
}
