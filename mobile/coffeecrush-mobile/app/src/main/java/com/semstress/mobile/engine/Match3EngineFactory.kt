package com.semstress.mobile.engine

import com.semstress.mobile.domain.StageConfig
import javax.inject.Inject

interface Match3EngineFactory {
    fun create(stage: StageConfig): Match3Engine
}

class DefaultMatch3EngineFactory @Inject constructor() : Match3EngineFactory {
    override fun create(stage: StageConfig): Match3Engine = Match3Engine(stage)
}
