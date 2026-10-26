package app.vigil.feature.settings.data.repository

import androidx.datastore.core.DataStore
import app.vigil.common.core.AppResult
import app.vigil.common.datastore.asStoreResult
import app.vigil.common.datastore.safeStoreCall
import app.vigil.feature.settings.data.SettingsRepository
import app.vigil.feature.settings.data.ThemeModeData
import app.vigil.feature.settings.data.local.SettingsPreferences
import app.vigil.feature.settings.data.local.StoredThemeMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class SettingsRepositoryImpl @Inject constructor(private val store: DataStore<SettingsPreferences>) :
    SettingsRepository {

    override fun observeThemeMode(): Flow<AppResult<ThemeModeData>> =
        store.data.map { it.themeMode.toData() }.distinctUntilChanged().asStoreResult()

    override suspend fun setThemeMode(mode: ThemeModeData): AppResult<Unit> = safeStoreCall {
        store.updateData { it.copy(themeMode = mode.toStored()) }
        Unit
    }
}

private fun StoredThemeMode.toData() = when (this) {
    StoredThemeMode.System -> ThemeModeData.System
    StoredThemeMode.Light -> ThemeModeData.Light
    StoredThemeMode.Dark -> ThemeModeData.Dark
}

private fun ThemeModeData.toStored() = when (this) {
    ThemeModeData.System -> StoredThemeMode.System
    ThemeModeData.Light -> StoredThemeMode.Light
    ThemeModeData.Dark -> StoredThemeMode.Dark
}
