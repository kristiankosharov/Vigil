package app.vigil.feature.applock.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.designsystem.PrimaryButton
import app.vigil.common.ui.designsystem.TertiaryButton
import app.vigil.common.ui.theme.statusColors
import app.vigil.feature.applock.ui.R

private val Benefits = listOf(
    R.string.applock_biometric_benefit_touch,
    R.string.applock_biometric_benefit_backup,
    R.string.applock_biometric_benefit_turn_off,
)

/** Step 3: what fingerprint unlock does, before the user decides (docs/design/README.md, 1.3). */
@Composable
internal fun EnableBiometricContent(stepCount: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        FingerprintBadge(Modifier.padding(top = 64.dp))
        OnboardingHeading(
            step = stepCount,
            stepCount = stepCount,
            title = stringResource(R.string.applock_biometric_setup_title),
            message = stringResource(R.string.applock_biometric_setup_message),
            modifier = Modifier.padding(top = 40.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Benefits.forEach { Benefit(stringResource(it)) }
        }
    }
}

@Composable
private fun FingerprintBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(148.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_fingerprint),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(60.dp),
            )
        }
    }
}

@Composable
private fun Benefit(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_check),
            contentDescription = null,
            tint = MaterialTheme.statusColors.success,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "Enable fingerprint" asks for a fingerprint first; "Not now" saves the PIN without it. */
@Composable
internal fun EnableBiometricButtons(
    isSaving: Boolean,
    onEnable: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryButton(
            text = stringResource(R.string.applock_biometric_enable),
            onClick = onEnable,
            enabled = !isSaving,
            icon = ImageVector.vectorResource(CommonR.drawable.common_ic_fingerprint),
        )
        TertiaryButton(
            text = stringResource(R.string.applock_biometric_not_now),
            onClick = onSkip,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
