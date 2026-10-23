package app.vigil.feature.home.data.repository

import app.vigil.common.core.AppResult
import app.vigil.common.database.asDbResult
import app.vigil.common.database.home.HomeEntryDao
import app.vigil.common.database.home.HomeEntryEntity
import app.vigil.feature.home.data.HomeEntryData
import app.vigil.feature.home.data.HomeRepository
import app.vigil.feature.home.data.mapper.toData
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class HomeRepositoryImpl @Inject constructor(private val dao: HomeEntryDao) : HomeRepository {

    override fun observeEntries(): Flow<AppResult<List<HomeEntryData>>> = dao.observeAll()
        .map { entities -> entities.map(HomeEntryEntity::toData) }
        .asDbResult()
}
