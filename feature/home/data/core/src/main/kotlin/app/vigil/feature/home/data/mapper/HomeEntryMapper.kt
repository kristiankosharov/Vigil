package app.vigil.feature.home.data.mapper

import app.vigil.common.database.home.HomeEntryEntity
import app.vigil.feature.home.data.HomeEntryData

internal fun HomeEntryEntity.toData() = HomeEntryData(id = id, title = title)
