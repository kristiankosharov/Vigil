package app.vigil.common.core

/**
 * Clears one store of user data on logout. :common:database and every feature that keeps user data
 * in DataStore contribute one with `@IntoSet`; logout runs them all.
 */
fun interface UserDataCleaner {
    suspend fun clear()
}
