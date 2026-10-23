package app.vigil.feature.home.domain.mapper

import app.vigil.feature.home.data.HomeEntryData
import app.vigil.feature.home.domain.model.HomeEntry

internal fun HomeEntryData.toDomain() = HomeEntry(id = id, title = title)
