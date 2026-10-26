package app.vigil.common.core

/**
 * Clears one store of user data on logout. Every feature that keeps user data contributes one with
 * `@IntoSet`; logout runs them all.
 */
fun interface UserDataCleaner {
    suspend fun clear()
}
