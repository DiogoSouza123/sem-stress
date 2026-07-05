package com.semstress.mobile.engine

import com.semstress.mobile.domain.Position
import kotlin.random.Random

class Match3Board(
    val rows: Int,
    val cols: Int,
    val pieceTypes: Int,
    seed: Long? = null
) {
    private val random = if (seed == null) Random.Default else Random(seed)
    private val cells: Array<IntArray> = Array(rows) { IntArray(cols) }

    fun fillRandom() {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                cells[row][col] = nextPiece()
            }
        }
    }

    fun get(row: Int, col: Int): Int = cells[row][col]

    fun set(row: Int, col: Int, value: Int) {
        cells[row][col] = value
    }

    fun swap(first: Position, second: Position) {
        val temp = cells[first.row][first.col]
        cells[first.row][first.col] = cells[second.row][second.col]
        cells[second.row][second.col] = temp
    }

    fun isValid(position: Position): Boolean {
        return position.row in 0 until rows && position.col in 0 until cols
    }

    fun nextPiece(): Int = random.nextInt(pieceTypes)

    fun snapshot(): List<List<Int>> {
        return cells.map { row -> row.toList() }
    }

    fun overwrite(snapshot: List<List<Int>>) {
        if (snapshot.size != rows || snapshot.any { it.size != cols }) {
            throw IllegalArgumentException("Invalid snapshot for ${rows}x$cols board.")
        }
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                cells[row][col] = snapshot[row][col]
            }
        }
    }
}
