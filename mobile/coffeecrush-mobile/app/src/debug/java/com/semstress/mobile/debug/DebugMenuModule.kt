package com.semstress.mobile.debug

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugMenuModule {
    @Binds
    abstract fun bindDebugMenuHost(impl: RealDebugMenuHost): DebugMenuHost
}
