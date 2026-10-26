package app.vigil.common.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.vigil.common.ui.R

// Manrope is bundled as one variable font (res/font/manrope.ttf, SIL OFL: common/ui/MANROPE_OFL.txt);
// each weight the type scale uses is an instance of its wght axis.
private val ManropeWeights = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
)

internal val Manrope = FontFamily(
    ManropeWeights.map { weight ->
        Font(
            resId = R.font.manrope,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

private val Baseline = Typography()

private fun TextStyle.manrope(weight: FontWeight? = fontWeight) = copy(fontFamily = Manrope, fontWeight = weight)

// Material 3 type scale in Manrope, with the heavier weights from the design (docs/design/README.md).
internal val AppTypography = Typography(
    displayLarge = Baseline.displayLarge.manrope(),
    displayMedium = Baseline.displayMedium.manrope(),
    displaySmall = Baseline.displaySmall.manrope(),
    headlineLarge = Baseline.headlineLarge.manrope(FontWeight.ExtraBold),
    headlineMedium = Baseline.headlineMedium.manrope(FontWeight.Bold),
    headlineSmall = Baseline.headlineSmall.manrope(FontWeight.Bold),
    titleLarge = Baseline.titleLarge.manrope(FontWeight.ExtraBold),
    titleMedium = Baseline.titleMedium.manrope(FontWeight.Bold),
    titleSmall = Baseline.titleSmall.manrope(FontWeight.Bold),
    bodyLarge = Baseline.bodyLarge.manrope(),
    bodyMedium = Baseline.bodyMedium.manrope(),
    bodySmall = Baseline.bodySmall.manrope(),
    labelLarge = Baseline.labelLarge.manrope(FontWeight.Bold),
    labelMedium = Baseline.labelMedium.manrope(FontWeight.Bold),
    labelSmall = Baseline.labelSmall.manrope(FontWeight.Bold),
)
