package app.vigil.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import app.vigil.R
import app.vigil.common.ui.R as CommonR
import app.vigil.feature.documents.ui.DocumentsRoute
import app.vigil.feature.settings.ui.account.AccountRoute

/** The main tabs, in bar order. TODO(reminders): add Reminders between them with its feature. */
private enum class MainTab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    Documents(R.string.tab_documents, CommonR.drawable.common_ic_document),
    Account(R.string.tab_account, CommonR.drawable.common_ic_user),
}

/**
 * The tabs and the bottom navigation bar (docs/design/README.md, "Bottom navigation"). Each tab
 * keeps its scroll and input state while another one is shown; Back on another tab returns to
 * Documents. Destinations opened from a tab cover the whole screen, bar included.
 */
@Composable
internal fun MainTabs(onOpenDocument: (String) -> Unit, onOpenAppearance: () -> Unit, modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableStateOf(MainTab.Documents) }
    val tabStates = rememberSaveableStateHolder()
    BackHandler(enabled = selected != MainTab.Documents) { selected = MainTab.Documents }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0), // each tab handles the status bar itself
        bottomBar = { MainNavigationBar(selected = selected, onSelect = { selected = it }) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            tabStates.SaveableStateProvider(selected.name) {
                when (selected) {
                    MainTab.Documents -> DocumentsRoute(onOpenDocument = onOpenDocument)
                    MainTab.Account -> AccountRoute(onOpenAppearance = onOpenAppearance)
                }
            }
        }
    }
}

@Composable
private fun MainNavigationBar(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier) {
        HorizontalDivider(color = scheme.surfaceContainerHighest)
        NavigationBar(containerColor = scheme.surfaceContainerLowest) {
            MainTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    icon = { Icon(ImageVector.vectorResource(tab.icon), contentDescription = null) },
                    label = { Text(stringResource(tab.label)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = scheme.onPrimaryContainer,
                        selectedTextColor = scheme.onSurface,
                        indicatorColor = scheme.primaryContainer,
                        unselectedIconColor = scheme.onSurfaceVariant,
                        unselectedTextColor = scheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
