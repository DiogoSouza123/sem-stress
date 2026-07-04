package com.semstress.mobile.domain

data class StageCatalog(
    val stages: List<StageConfig>,
    val menuMusicName: String,
    val menuMusicVolumePercent: Int
)
