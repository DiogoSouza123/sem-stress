package com.semstress.mobile.domain

/**
 * GP-02: an optional secondary objective ("Pedido do cliente: N grãos de [pieceType]"), combined
 * with the always-present `score` objective. Only one objective type is introduced in this pass;
 * `deliver`/`clean`/`unfreeze`/`cascade` are documented in gameplay.md for future work.
 */
data class CollectObjective(
    val pieceType: Int,
    val count: Int
) {
    fun isComplete(collected: Int): Boolean = collected >= count
}
