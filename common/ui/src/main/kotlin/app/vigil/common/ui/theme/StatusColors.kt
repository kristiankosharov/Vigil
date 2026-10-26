// Hex color literals are the design tokens themselves; naming each number would add nothing.
@file:Suppress("MagicNumber")

package app.vigil.common.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Status roles Material 3 doesn't have (docs/design/README.md, "Status colors"). Always pair them
 * with an icon, shape or text: color alone never carries the meaning.
 */
@Immutable
data class StatusColors(
    /** Done and OK: "Signed", "PINs match". */
    val success: Color,
    /** Needs action soon: "To sign". */
    val warning: Color,
)

internal val LightStatusColors = StatusColors(success = Color(0xFF067647), warning = Color(0xFFB54708))
internal val DarkStatusColors = StatusColors(success = Color(0xFF47CD89), warning = Color(0xFFFDB022))

internal val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/** The status colors of the current [AppTheme]. */
val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current
