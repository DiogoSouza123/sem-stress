package com.semstress;

import java.util.Random;

public class Board {
    public static final int ROWS = 8;
    public static final int COLS = 6;
    public static final int PIECE_TYPES = 5;

    private final int[][] cells;
    private final Random random;

    public Board() {
        this(new Random());
    }

    public Board(Random random) {
        this.random = random;
        this.cells = new int[ROWS][COLS];
    }

    public void fillRandom() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                cells[row][col] = nextPiece();
            }
        }
    }

    public int get(int row, int col) {
        return cells[row][col];
    }

    public void set(int row, int col, int value) {
        cells[row][col] = value;
    }

    public void swap(Position first, Position second) {
        int temp = cells[first.getRow()][first.getCol()];
        cells[first.getRow()][first.getCol()] = cells[second.getRow()][second.getCol()];
        cells[second.getRow()][second.getCol()] = temp;
    }

    public int nextPiece() {
        return random.nextInt(PIECE_TYPES);
    }
}
