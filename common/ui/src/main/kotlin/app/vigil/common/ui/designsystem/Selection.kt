package app.vigil.common.ui.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.vigil.common.ui.theme.AppTheme

private const val DISABLED_ALPHA = 0.38f

/**
 * Filter or option chip with a pill shape. Selected chips use `inverseSurface`, matching the filter
 * row on the Documents tab. [isError] marks a required group the user left empty.
 */
@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    val scheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = FilterChipDefaults.filterChipColors(
            labelColor = if (isError) scheme.error else scheme.onSurfaceVariant,
            selectedContainerColor = scheme.inverseSurface,
            selectedLabelColor = scheme.inverseOnSurface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = if (isError) scheme.error else scheme.outlineVariant,
        ),
        interactionSource = interactionSource,
    )
}

/**
 * Settings row with a switch; the whole row is the toggle target. [supportingText] with `isError = true`
 * explains why the setting can't be on (e.g. no fingerprint enrolled on the device).
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val source = interactionSource ?: remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = source,
                indication = null,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        leadingIcon?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            if (supportingText != null) {
                Text(
                    supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) scheme.error else scheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, interactionSource = source)
    }
}

/**
 * Checkbox with a label; the whole row is the toggle target. Used for consent before signing.
 * When [isError] is true, [errorText] is shown under the row.
 */
@Composable
fun CheckboxRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val source = interactionSource ?: remember { MutableInteractionSource() }
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    interactionSource = source,
                    indication = null,
                    onValueChange = onCheckedChange,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = if (isError) {
                    CheckboxDefaults.colors(uncheckedColor = scheme.error, checkedColor = scheme.error)
                } else {
                    CheckboxDefaults.colors()
                },
                interactionSource = source,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) scheme.onSurface else scheme.onSurface.copy(alpha = DISABLED_ALPHA),
            )
        }
        if (isError && errorText != null) {
            Text(
                errorText,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
                modifier = Modifier.padding(start = 36.dp),
            )
        }
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun SelectionPreview() {
    AppTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFilterChip(label = "To sign", selected = true, onClick = {})
                    AppFilterChip(label = "Signed", selected = false, onClick = {})
                    AppFilterChip(label = "Pick a type", selected = false, onClick = {}, isError = true)
                }
                SwitchRow(title = "Unlock with fingerprint", checked = true, onCheckedChange = {})
                SwitchRow(
                    title = "Unlock with fingerprint",
                    checked = false,
                    onCheckedChange = {},
                    isError = true,
                    supportingText = "Not set up on this device",
                )
                CheckboxRow(text = "I've read all 4 pages", checked = true, onCheckedChange = {})
                CheckboxRow(
                    text = "I've read all 4 pages",
                    checked = false,
                    onCheckedChange = {},
                    isError = true,
                    errorText = "Confirm you've read the document to sign it",
                )
            }
        }
    }
}
