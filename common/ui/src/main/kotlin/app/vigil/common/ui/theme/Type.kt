package app.vigil.common.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

private val Baseline = Typography()

// Material 3 type scale with the heavier weights from the design. The design uses Manrope;
// TODO(design): bundle Manrope in res/font and set it as the font family of every style here.
internal val AppTypography = Typography(
    headlineLarge = Baseline.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = Baseline.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = Baseline.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Baseline.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    titleMedium = Baseline.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = Baseline.titleSmall.copy(fontWeight = FontWeight.Bold),
    labelLarge = Baseline.labelLarge.copy(fontWeight = FontWeight.Bold),
    labelMedium = Baseline.labelMedium.copy(fontWeight = FontWeight.Bold),
    labelSmall = Baseline.labelSmall.copy(fontWeight = FontWeight.Bold),
)
