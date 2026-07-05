package com.semstress.mobile.debug

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DebugMenuModule {
    @Provides
    fun provideDebugMenuHost(): DebugMenuHost = NoOpDebugMenuHost
}
