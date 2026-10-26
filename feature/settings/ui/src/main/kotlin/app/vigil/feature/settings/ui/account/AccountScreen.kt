package app.vigil.feature.settings.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.settings.domain.model.ThemeMode
import app.vigil.feature.settings.ui.R

/** The Account tab. */
@Composable
fun AccountRoute(onOpenAppearance: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: AccountViewModel = hiltViewModel()
    AccountScreen(state = viewModel.uiState(), onOpenAppearance = onOpenAppearance, modifier = modifier)
}

@Composable
internal fun AccountScreen(state: AccountUiState, onOpenAppearance: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        // The tab bar below already handles the bottom inset.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_account_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.02).em,
                ),
                modifier = Modifier.semantics { heading() },
            )
            AppearanceRow(themeMode = state.themeMode, onClick = onOpenAppearance)
        }
    }
}

/** Opens Appearance; shows the current choice on the right. */
@Composable
private fun AppearanceRow(themeMode: ThemeMode?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_appearance),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.settings_appearance_title),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
            )
            if (themeMode != null) {
                Text(
                    text = stringResource(themeMode.shortLabel()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun ThemeMode.shortLabel() = when (this) {
    ThemeMode.System -> R.string.settings_theme_system_short
    ThemeMode.Light -> R.string.settings_theme_light
    ThemeMode.Dark -> R.string.settings_theme_dark
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun AccountPreview() {
    AppTheme { AccountScreen(state = AccountUiState(themeMode = ThemeMode.System), onOpenAppearance = {}) }
}
