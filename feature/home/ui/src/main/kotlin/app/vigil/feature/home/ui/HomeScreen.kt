package app.vigil.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.vigil.common.ui.mvi.UiEffectHandler
import app.vigil.common.ui.theme.AppTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun HomeRoute(modifier: Modifier = Modifier) {
    val viewModel: HomeViewModel = hiltViewModel()
    HomeScreen(state = viewModel.uiState(), onAction = viewModel::onAction, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(state: HomeUiState, onAction: (HomeAction) -> Unit, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }

    UiEffectHandler(
        effect = state.effect,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onEffectConsumed = { onAction(HomeAction.EffectConsumed(it)) },
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.home_title), modifier = Modifier.semantics { heading() })
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        when {
            state.isLoading -> HomeLoading(contentModifier)
            state.entries.isEmpty() -> HomeEmpty(contentModifier)
            else -> HomeEntries(state.entries, contentModifier)
        }
    }
}

@Composable
private fun HomeLoading(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.home_loading)
    Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = description })
    }
}

@Composable
private fun HomeEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

// TODO(home): design the real entry row; this placeholder shows only the title.
@Composable
private fun HomeEntries(entries: ImmutableList<HomeEntryItem>, modifier: Modifier = Modifier) {
    LazyColumn(modifier) {
        items(entries, key = { it.id }) { entry ->
            ListItem(headlineContent = { Text(entry.title) })
        }
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun HomeScreenEmptyPreview() {
    AppTheme(dynamicColor = false) {
        HomeScreen(state = HomeUiState(isLoading = false, entries = persistentListOf(), effect = null), onAction = {})
    }
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun HomeScreenEntriesPreview() {
    val entries = persistentListOf(HomeEntryItem("1", "First entry"), HomeEntryItem("2", "Second entry"))
    AppTheme(dynamicColor = false) {
        HomeScreen(state = HomeUiState(isLoading = false, entries = entries, effect = null), onAction = {})
    }
}
