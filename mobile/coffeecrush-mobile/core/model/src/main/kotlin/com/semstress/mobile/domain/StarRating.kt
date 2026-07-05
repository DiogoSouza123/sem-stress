package com.semstress.mobile.domain

private const val TWO_STAR_SCORE_RATIO = 1.5f
private const val THREE_STAR_SCORE_RATIO = 2.0f
private const val THREE_STAR_MOVES_LEFT_RATIO = 0.3f
private const val ONE_STAR = 1
private const val TWO_STARS = 2
private const val THREE_STARS = 3

/**
 * GP-03: 1 star for clearing the stage's objective (today, only `score`); 2 for a higher score
 * threshold; 3 for either a much higher score threshold or finishing with a healthy fraction of
 * moves still unused (an efficient clear). Returns 0 when the stage wasn't won.
 */
fun calculateStars(
    won: Boolean,
    score: Int,
    targetScore: Int,
    movesRemaining: Int,
    initialMoves: Int
): Int {
    if (!won) {
        return 0
    }

    val scoreRatio = if (targetScore > 0) score.toFloat() / targetScore else 1f
    val movesLeftRatio = if (initialMoves > 0) movesRemaining.toFloat() / initialMoves else 0f
    val qualifiesForThreeStars = scoreRatio >= THREE_STAR_SCORE_RATIO || movesLeftRatio >= THREE_STAR_MOVES_LEFT_RATIO

    return when {
        scoreRatio >= TWO_STAR_SCORE_RATIO && qualifiesForThreeStars -> THREE_STARS
        scoreRatio >= TWO_STAR_SCORE_RATIO -> TWO_STARS
        else -> ONE_STAR
    }
}
