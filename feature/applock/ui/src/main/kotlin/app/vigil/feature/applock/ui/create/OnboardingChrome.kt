package app.vigil.feature.applock.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.vigil.common.ui.R as CommonR
import app.vigil.feature.applock.ui.R

/** Back arrow and one progress segment per onboarding step. */
@Composable
internal fun OnboardingTopBar(step: Int, stepCount: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val progress = stringResource(R.string.applock_step_label, step, stepCount)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 32.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_back),
                contentDescription = stringResource(R.string.applock_back),
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = progress },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(stepCount) { index ->
                val color = if (index < step) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(color, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

/** "STEP 1 OF 3", the step's title and its explanation, centered. */
@Composable
internal fun OnboardingHeading(
    step: Int,
    stepCount: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.applock_step_label, step, stepCount).uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.06.em),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 10.dp, bottom = 8.dp)
                .semantics { heading() },
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
