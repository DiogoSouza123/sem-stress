package com.semstress.mobile.engine

/**
 * Builds a [Match3Board] from a literal matrix, e.g.:
 * ```
 * boardFrom(
 *     """
 *     1 2 3 4
 *     2 1 1 3
 *     1 3 4 2
 *     """
 * )
 * ```
 * `seed` controls the RNG used for refills/shuffles triggered during the test.
 */
fun boardFrom(pattern: String, pieceTypes: Int? = null, seed: Long? = 42L): Match3Board {
    val matrix = pattern.trimIndent().lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { line -> line.split(Regex("\\s+")).map { it.toInt() } }
    require(matrix.isNotEmpty()) { "Pattern vazio." }
    val cols = matrix[0].size
    require(matrix.all { it.size == cols }) { "Todas as linhas do pattern devem ter o mesmo numero de colunas." }

    val resolvedTypes = pieceTypes ?: ((matrix.flatten().maxOrNull() ?: 0) + 1)
    val board = Match3Board(rows = matrix.size, cols = cols, pieceTypes = resolvedTypes, seed = seed)
    matrix.forEachIndexed { row, values ->
        values.forEachIndexed { col, value -> board.set(row, col, value) }
    }
    return board
}

fun hasAnyMatch(board: Match3Board, minMatchSize: Int = 3): Boolean {
    val horizontalMatch = (0 until board.rows).any { row ->
        hasRunOfAtLeast(board.cols, minMatchSize) { col -> board.get(row, col) }
    }
    val verticalMatch = (0 until board.cols).any { col ->
        hasRunOfAtLeast(board.rows, minMatchSize) { row -> board.get(row, col) }
    }
    return horizontalMatch || verticalMatch
}

private fun hasRunOfAtLeast(size: Int, minLength: Int, valueAt: (Int) -> Int): Boolean {
    var runLength = 1
    for (i in 1 until size) {
        runLength = if (valueAt(i) == valueAt(i - 1)) runLength + 1 else 1
        if (runLength >= minLength) return true
    }
    return false
}
