package com.semstress.mobile.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object MenuRoute

@Serializable
data class GameRoute(val stageId: Int, val zen: Boolean = false, val dailySeed: Long? = null)
