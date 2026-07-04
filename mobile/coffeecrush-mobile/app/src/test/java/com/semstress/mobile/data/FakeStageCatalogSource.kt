package com.semstress.mobile.data

import com.semstress.mobile.domain.StageCatalog

class FakeStageCatalogSource(private val catalog: StageCatalog) : StageCatalogSource {
    override suspend fun load(): StageCatalog = catalog
}
