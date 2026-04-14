package com.semstress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameEngine {
    private static final int MATCH3_POINTS = 500;
    private static final int MATCH4_OR_MORE_POINTS = 1000;
    private static final int EMPTY = -1;

    public MoveResult tryMove(Board board, Position first, Position second) {
        if (!isAdjacent(first, second)) {
            return new MoveResult(false, 0);
        }

        board.swap(first, second);

        Set<Position> initialMatches = findMatchedCells(board);
        if (initialMatches.isEmpty()) {
            board.swap(first, second);
            return new MoveResult(false, 0);
        }

        int points = resolveBoard(board);
        return new MoveResult(true, points);
    }

    public int resolveBoard(Board board) {
        int totalPoints = 0;

        while (true) {
            List<Integer> runLengths = findRunLengths(board);
            if (runLengths.isEmpty()) {
                return totalPoints;
            }

            Set<Position> matched = findMatchedCells(board);
            totalPoints += calculatePoints(runLengths);
            clearMatched(board, matched);
            collapseAndRefill(board);
        }
    }

    private int calculatePoints(List<Integer> runLengths) {
        int points = 0;
        for (Integer runLength : runLengths) {
            points += runLength >= 4 ? MATCH4_OR_MORE_POINTS : MATCH3_POINTS;
        }
        return points;
    }

    private boolean isAdjacent(Position first, Position second) {
        int rowDiff = Math.abs(first.getRow() - second.getRow());
        int colDiff = Math.abs(first.getCol() - second.getCol());
        return (rowDiff + colDiff) == 1;
    }

    private void clearMatched(Board board, Set<Position> matched) {
        for (Position position : matched) {
            board.set(position.getRow(), position.getCol(), EMPTY);
        }
    }

    private void collapseAndRefill(Board board) {
        for (int col = 0; col < Board.COLS; col++) {
            List<Integer> pieces = new ArrayList<>();
            for (int row = Board.ROWS - 1; row >= 0; row--) {
                int value = board.get(row, col);
                if (value != EMPTY) {
                    pieces.add(value);
                }
            }

            int row = Board.ROWS - 1;
            for (Integer piece : pieces) {
                board.set(row, col, piece);
                row--;
            }

            while (row >= 0) {
                board.set(row, col, board.nextPiece());
                row--;
            }
        }
    }

    private Set<Position> findMatchedCells(Board board) {
        Set<Position> matched = new HashSet<>();

        // Horizontal runs
        for (int row = 0; row < Board.ROWS; row++) {
            int col = 0;
            while (col < Board.COLS) {
                int start = col;
                int value = board.get(row, col);
                while (col + 1 < Board.COLS && board.get(row, col + 1) == value) {
                    col++;
                }
                int length = col - start + 1;
                if (length >= 3) {
                    for (int c = start; c <= col; c++) {
                        matched.add(new Position(row, c));
                    }
                }
                col++;
            }
        }

        // Vertical runs
        for (int col = 0; col < Board.COLS; col++) {
            int row = 0;
            while (row < Board.ROWS) {
                int start = row;
                int value = board.get(row, col);
                while (row + 1 < Board.ROWS && board.get(row + 1, col) == value) {
                    row++;
                }
                int length = row - start + 1;
                if (length >= 3) {
                    for (int r = start; r <= row; r++) {
                        matched.add(new Position(r, col));
                    }
                }
                row++;
            }
        }

        return matched;
    }

    private List<Integer> findRunLengths(Board board) {
        List<Integer> lengths = new ArrayList<>();

        // Horizontal runs
        for (int row = 0; row < Board.ROWS; row++) {
            int col = 0;
            while (col < Board.COLS) {
                int start = col;
                int value = board.get(row, col);
                while (col + 1 < Board.COLS && board.get(row, col + 1) == value) {
                    col++;
                }
                int length = col - start + 1;
                if (length >= 3) {
                    lengths.add(length);
                }
                col++;
            }
        }

        // Vertical runs
        for (int col = 0; col < Board.COLS; col++) {
            int row = 0;
            while (row < Board.ROWS) {
                int start = row;
                int value = board.get(row, col);
                while (row + 1 < Board.ROWS && board.get(row + 1, col) == value) {
                    row++;
                }
                int length = row - start + 1;
                if (length >= 3) {
                    lengths.add(length);
                }
                row++;
            }
        }

        return lengths;
    }
}
