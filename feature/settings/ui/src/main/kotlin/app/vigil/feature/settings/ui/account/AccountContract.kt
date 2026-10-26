package app.vigil.feature.settings.ui.account

import androidx.compose.runtime.Immutable
import app.vigil.common.ui.mvi.UiAction
import app.vigil.common.ui.mvi.UiState
import app.vigil.feature.settings.domain.model.ThemeMode

/**
 * The Account tab (docs/design/README.md, 2.3). For now only Appearance.
 * TODO(account): profile, Security, Sign-in methods and Sign out from the design.
 */
@Immutable
internal data class AccountUiState(
    /** Null until the stored choice is read. */
    val themeMode: ThemeMode?,
) : UiState

/** Everything on this screen is navigation, which goes straight to the Route's callbacks. */
internal sealed interface AccountAction : UiAction
