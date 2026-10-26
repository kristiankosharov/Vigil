package app.vigil.feature.settings.ui.appearance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.vigil.common.ui.R as CommonR
import app.vigil.common.ui.mvi.UiEffectHandler
import app.vigil.common.ui.theme.AppTheme
import app.vigil.feature.settings.domain.model.ThemeMode
import app.vigil.feature.settings.ui.R

@Composable
fun AppearanceRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: AppearanceViewModel = hiltViewModel()
    AppearanceScreen(state = viewModel.uiState(), onAction = viewModel::onAction, onBack = onBack, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceScreen(
    state: AppearanceUiState,
    onAction: (AppearanceAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { onAction(AppearanceAction.EffectConsumed(it)) },
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_appearance_title),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = ImageVector.vectorResource(CommonR.drawable.common_ic_back),
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_appearance_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ThemeOptions(selected = state.selected, onSelect = { onAction(AppearanceAction.Select(it)) })
            Text(
                text = stringResource(R.string.settings_appearance_security_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeOptions(selected: ThemeMode?, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                ThemeOption(mode = mode, selected = mode == selected, onSelect = { onSelect(mode) })
            }
        }
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val (title, description) = when (mode) {
        ThemeMode.System -> R.string.settings_theme_system to R.string.settings_theme_system_description
        ThemeMode.Light -> R.string.settings_theme_light to null
        ThemeMode.Dark -> R.string.settings_theme_dark to null
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            if (description != null) {
                Text(
                    stringResource(description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun AppearanceScreenPreview() {
    AppTheme(dynamicColor = false) {
        AppearanceScreen(
            state = AppearanceUiState(selected = ThemeMode.System, effect = null),
            onAction = {},
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun AppearanceScreenLoadingPreview() {
    AppTheme(dynamicColor = false) {
        AppearanceScreen(state = AppearanceUiState(selected = null, effect = null), onAction = {}, onBack = {})
    }
}
