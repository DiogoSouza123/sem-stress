package com.semstress.mobile.data

import com.semstress.mobile.domain.PlayerProgress

class FakeProgressStore(initial: PlayerProgress = PlayerProgress()) : ProgressStore {
    var saved: PlayerProgress = initial
        private set
    var saveCount: Int = 0
        private set

    override fun load(totalStages: Int): PlayerProgress = saved

    override fun save(progress: PlayerProgress) {
        saved = progress
        saveCount++
    }
}
