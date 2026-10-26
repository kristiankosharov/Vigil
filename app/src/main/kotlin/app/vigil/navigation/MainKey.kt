package app.vigil.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// One key per destination. Arguments travel in the key: IDs and primitives only, never models.

/** The main tabs (Documents, Account) with the bottom navigation bar. */
@Serializable
data object MainKey : NavKey
