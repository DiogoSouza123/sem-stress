package com.semstress;

public final class BoardTestUtils {

    private BoardTestUtils() {
    }

    public static Board boardFromPattern(TestRandom random) {
        Board board = new Board(random);
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                board.set(row, col, (row + col) % Board.PIECE_TYPES);
            }
        }
        return board;
    }

    public static int[][] snapshot(Board board) {
        int[][] snap = new int[Board.ROWS][Board.COLS];
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                snap[row][col] = board.get(row, col);
            }
        }
        return snap;
    }

    public static void assertBoardEquals(int[][] expected, Board board) {
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                org.junit.Assert.assertEquals(
                        "Diferenca em [" + row + "][" + col + "]",
                        expected[row][col],
                        board.get(row, col)
                );
            }
        }
    }

    public static boolean hasAnyMatch(Board board) {
        // horizontal
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col <= Board.COLS - 3; col++) {
                int a = board.get(row, col);
                int b = board.get(row, col + 1);
                int c = board.get(row, col + 2);
                if (a == b && b == c) {
                    return true;
                }
            }
        }

        // vertical
        for (int col = 0; col < Board.COLS; col++) {
            for (int row = 0; row <= Board.ROWS - 3; row++) {
                int a = board.get(row, col);
                int b = board.get(row + 1, col);
                int c = board.get(row + 2, col);
                if (a == b && b == c) {
                    return true;
                }
            }
        }

        return false;
    }
}
