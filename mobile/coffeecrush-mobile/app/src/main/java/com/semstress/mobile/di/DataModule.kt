package com.semstress.mobile.di

import com.semstress.mobile.data.ProgressRepository
import com.semstress.mobile.data.ProgressStore
import com.semstress.mobile.data.SettingsRepository
import com.semstress.mobile.data.SettingsStore
import com.semstress.mobile.data.StageCatalogSource
import com.semstress.mobile.data.StageRepository
import com.semstress.mobile.engine.DefaultMatch3EngineFactory
import com.semstress.mobile.engine.Match3EngineFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindStageCatalogSource(impl: StageRepository): StageCatalogSource

    @Binds
    abstract fun bindProgressStore(impl: ProgressRepository): ProgressStore

    @Binds
    abstract fun bindSettingsStore(impl: SettingsRepository): SettingsStore

    @Binds
    abstract fun bindMatch3EngineFactory(impl: DefaultMatch3EngineFactory): Match3EngineFactory
}
